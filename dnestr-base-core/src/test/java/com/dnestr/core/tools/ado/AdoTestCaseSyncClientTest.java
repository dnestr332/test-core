package com.dnestr.core.tools.ado;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdoTestCaseSyncClientTest {

    private HttpServer server;
    private volatile HttpHandler handler;
    private AdoTestCaseSyncClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> handler.handle(exchange));
        server.start();

        RequestSpecification spec = RestAssured.given()
                .baseUri("http://localhost:" + server.getAddress().getPort())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON);

        AdoSyncConfig config = new AdoSyncConfig("proj", 100, 5);
        client = new AdoTestCaseSyncClient(spec, config);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private void respondJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    private String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    @Test
    void resolveOrCreateSprintSuite_returnsExistingSuite_whenNameAndParentMatch() {
        handler = exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {
                respondJson(exchange, 200,
                        "{\"value\":[{\"id\":10,\"name\":\"Sprint1\",\"parentSuite\":{\"id\":5}}]}");
            } else {
                throw new AssertionError("suite already exists - should not create one");
            }
        };

        int suiteId = client.resolveOrCreateSprintSuite("Sprint1");

        assertThat(suiteId).isEqualTo(10);
    }

    @Test
    void resolveOrCreateSprintSuite_ignoresSameNamedSuite_underDifferentParent() {
        handler = exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {
                respondJson(exchange, 200,
                        "{\"value\":[{\"id\":10,\"name\":\"Sprint1\",\"parentSuite\":{\"id\":999}}]}");
            } else {
                respondJson(exchange, 200, "{\"id\":77}");
            }
        };

        int suiteId = client.resolveOrCreateSprintSuite("Sprint1");

        assertThat(suiteId).isEqualTo(77);
    }

    @Test
    void resolveOrCreateSprintSuite_creates_whenNoSuiteMatches() {
        String[] capturedBody = new String[1];
        handler = exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {
                respondJson(exchange, 200, "{\"value\":[]}");
            } else {
                capturedBody[0] = readBody(exchange);
                respondJson(exchange, 200, "{\"id\":77}");
            }
        };

        int suiteId = client.resolveOrCreateSprintSuite("Sprint1");

        assertThat(suiteId).isEqualTo(77);
        assertThat(capturedBody[0])
                .contains("\"name\":\"Sprint1\"")
                .contains("\"staticTestSuite\"");
    }

    @Test
    void createTestCase_postsTitleAndSteps_andReturnsCreatedId() {
        String[] capturedBody = new String[1];
        handler = exchange -> {
            capturedBody[0] = readBody(exchange);
            respondJson(exchange, 200, "{\"id\": 321}");
        };

        long testCaseId = client.createTestCase("My Scenario", "Given x\nWhen y");

        assertThat(testCaseId).isEqualTo(321L);
        assertThat(capturedBody[0])
                .contains("\"path\":\"/fields/System.Title\"")
                .contains("\"value\":\"My Scenario\"")
                .contains("\"path\":\"/fields/Microsoft.VSTS.TCM.Steps\"")
                .contains("Given x")
                .contains("When y");
    }

    @Test
    void createTestCase_throws_whenApiReturnsError() {
        handler = exchange -> respondJson(exchange, 400, "{\"message\":\"bad request\"}");

        assertThatThrownBy(() -> client.createTestCase("My Scenario", "Given x"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("create test case")
                .hasMessageContaining("400");
    }

    @Test
    void createTestCase_escapesXmlSpecialCharacters_inGherkinSteps() {
        String[] capturedBody = new String[1];
        handler = exchange -> {
            capturedBody[0] = readBody(exchange);
            respondJson(exchange, 200, "{\"id\": 1}");
        };

        client.createTestCase("Title", "Given a <tag> & \"quoted\" 'value'");

        assertThat(capturedBody[0])
                .contains("&lt;tag&gt;")
                .contains("&amp;")
                .contains("&quot;quoted&quot;")
                .contains("&apos;value&apos;");
    }

    @Test
    void addTestCaseToSuite_postsToConfiguredPlanAndSuite() {
        handler = exchange -> {
            assertThat(exchange.getRequestURI().getPath())
                    .isEqualTo("/proj/_apis/test/Plans/100/suites/55/testcases/9");
            respondJson(exchange, 200, "{}");
        };

        client.addTestCaseToSuite(55, 9L);
    }

    @Test
    void addTestCaseToSuite_throws_whenApiReturnsError() {
        handler = exchange -> respondJson(exchange, 500, "{\"message\":\"boom\"}");

        assertThatThrownBy(() -> client.addTestCaseToSuite(55, 9L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("add test case 9 to suite 55");
    }

    @Test
    void updateTestCase_patchesTitleAndSteps() {
        String[] capturedBody = new String[1];
        handler = exchange -> {
            assertThat(exchange.getRequestMethod()).isEqualTo("PATCH");
            capturedBody[0] = readBody(exchange);
            respondJson(exchange, 200, "{}");
        };

        client.updateTestCase(9L, "Updated Title", "Given z");

        assertThat(capturedBody[0])
                .contains("\"value\":\"Updated Title\"")
                .contains("Given z");
    }

    @Test
    void hasPbiLink_true_whenRelationWithMatchingUrlExists() {
        handler = exchange -> {
            if (exchange.getRequestURI().getQuery() != null
                    && exchange.getRequestURI().getQuery().contains("expand=relations")) {
                respondJson(exchange, 200,
                        "{\"relations\":[{\"rel\":\"System.LinkTypes.Related\","
                                + "\"url\":\"http://ado/proj/_apis/wit/workItems/42\"}]}");
            } else {
                respondJson(exchange, 200, "{\"url\":\"http://ado/proj/_apis/wit/workItems/42\"}");
            }
        };

        assertThat(client.hasPbiLink(9L, 42L)).isTrue();
    }

    @Test
    void hasPbiLink_false_whenNoRelationsPresent() {
        handler = exchange -> {
            if (exchange.getRequestURI().getQuery() != null
                    && exchange.getRequestURI().getQuery().contains("expand=relations")) {
                respondJson(exchange, 200, "{}");
            } else {
                respondJson(exchange, 200, "{\"url\":\"http://ado/proj/_apis/wit/workItems/42\"}");
            }
        };

        assertThat(client.hasPbiLink(9L, 42L)).isFalse();
    }

    @Test
    void hasPbiLink_false_whenRelationRelDoesNotMatch() {
        handler = exchange -> {
            if (exchange.getRequestURI().getQuery() != null
                    && exchange.getRequestURI().getQuery().contains("expand=relations")) {
                respondJson(exchange, 200,
                        "{\"relations\":[{\"rel\":\"System.LinkTypes.Hierarchy-Forward\","
                                + "\"url\":\"http://ado/proj/_apis/wit/workItems/42\"}]}");
            } else {
                respondJson(exchange, 200, "{\"url\":\"http://ado/proj/_apis/wit/workItems/42\"}");
            }
        };

        assertThat(client.hasPbiLink(9L, 42L)).isFalse();
    }

    @Test
    void linkTestCaseToPbi_skipsPatch_whenLinkAlreadyExists() {
        AtomicInteger patchCount = new AtomicInteger();
        handler = exchange -> {
            if ("PATCH".equals(exchange.getRequestMethod())) {
                patchCount.incrementAndGet();
                respondJson(exchange, 200, "{}");
            } else if (exchange.getRequestURI().getQuery() != null
                    && exchange.getRequestURI().getQuery().contains("expand=relations")) {
                respondJson(exchange, 200,
                        "{\"relations\":[{\"rel\":\"System.LinkTypes.Related\","
                                + "\"url\":\"http://ado/proj/_apis/wit/workItems/42\"}]}");
            } else {
                respondJson(exchange, 200, "{\"url\":\"http://ado/proj/_apis/wit/workItems/42\"}");
            }
        };

        client.linkTestCaseToPbi(9L, 42L);

        assertThat(patchCount.get()).isZero();
    }

    @Test
    void linkTestCaseToPbi_patchesRelatedLink_whenNotAlreadyLinked() {
        String[] capturedBody = new String[1];
        handler = exchange -> {
            if ("PATCH".equals(exchange.getRequestMethod())) {
                capturedBody[0] = readBody(exchange);
                respondJson(exchange, 200, "{}");
            } else if (exchange.getRequestURI().getQuery() != null
                    && exchange.getRequestURI().getQuery().contains("expand=relations")) {
                respondJson(exchange, 200, "{}");
            } else {
                respondJson(exchange, 200, "{\"url\":\"http://ado/proj/_apis/wit/workItems/42\"}");
            }
        };

        client.linkTestCaseToPbi(9L, 42L);

        assertThat(capturedBody[0])
                .contains("\"rel\":\"System.LinkTypes.Related\"")
                .contains("\"url\":\"http://ado/proj/_apis/wit/workItems/42\"")
                .contains("/relations/-");
    }

    @Test
    void linkTestCaseToPbi_fetchesPbiWorkItemUrlOnlyOnce_whenNotAlreadyLinked() {
        AtomicInteger workItemUrlLookups = new AtomicInteger();
        handler = exchange -> {
            String query = exchange.getRequestURI().getQuery();
            if ("PATCH".equals(exchange.getRequestMethod())) {
                respondJson(exchange, 200, "{}");
            } else if (query != null && query.contains("expand=relations")) {
                respondJson(exchange, 200, "{}");
            } else {
                workItemUrlLookups.incrementAndGet();
                respondJson(exchange, 200, "{\"url\":\"http://ado/proj/_apis/wit/workItems/42\"}");
            }
        };

        client.linkTestCaseToPbi(9L, 42L);

        assertThat(workItemUrlLookups.get()).isEqualTo(1);
    }
}
