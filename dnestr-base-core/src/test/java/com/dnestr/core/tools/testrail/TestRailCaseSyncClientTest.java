package com.dnestr.core.tools.testrail;

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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestRailCaseSyncClientTest {

    private HttpServer server;
    private volatile HttpHandler handler;
    private TestRailCaseSyncClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/index.php", exchange -> handler.handle(exchange));
        server.start();

        RequestSpecification spec = RestAssured.given()
                .baseUri("http://localhost:" + server.getAddress().getPort())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON);

        client = new TestRailCaseSyncClient(spec);
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
    void resolveProjectId_returnsProjectId_fromGetSuiteResponse() {
        handler = exchange -> {
            assertThat(exchange.getRequestURI().getQuery()).isEqualTo("/api/v2/get_suite/5");
            respondJson(exchange, 200, "{\"project_id\": 7}");
        };

        int projectId = client.resolveProjectId(5);

        assertThat(projectId).isEqualTo(7);
    }

    @Test
    void resolveProjectId_throws_whenApiReturnsError() {
        handler = exchange -> respondJson(exchange, 404, "{\"error\":\"not found\"}");

        assertThatThrownBy(() -> client.resolveProjectId(5))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("get_suite/5")
                .hasMessageContaining("404");
    }

    @Test
    void resolveGherkinTemplateId_findsTemplate_matchingBddHint() {
        handler = exchange -> respondJson(exchange, 200,
                "[{\"id\":1,\"name\":\"Test Case (Text)\"},{\"id\":9,\"name\":\"Behavior Driven Development\"}]");

        int templateId = client.resolveGherkinTemplateId(7);

        assertThat(templateId).isEqualTo(9);
    }

    @Test
    void resolveGherkinTemplateId_throws_whenNoTemplateMatches() {
        handler = exchange -> respondJson(exchange, 200, "[{\"id\":1,\"name\":\"Test Case (Text)\"}]");

        assertThatThrownBy(() -> client.resolveGherkinTemplateId(7))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No Gherkin/BDD template");
    }

    @Test
    void resolveField_matchesByLabelOrSystemName_andCachesAcrossCalls() {
        AtomicInteger requestCount = new AtomicInteger();
        handler = exchange -> {
            requestCount.incrementAndGet();
            respondJson(exchange, 200,
                    "[{\"system_name\":\"custom_gherkin\",\"label\":\"Gherkin\",\"type_id\":1},"
                            + "{\"system_name\":\"custom_auto\",\"label\":\"Is Automated\",\"type_id\":2}]");
        };

        Optional<TestRailCaseSyncClient.CaseField> byLabel = client.resolveField("gherkin");
        Optional<TestRailCaseSyncClient.CaseField> bySecondHint = client.resolveField("nonsense", "is automated");

        assertThat(byLabel).isPresent();
        assertThat(byLabel.get().systemName()).isEqualTo("custom_gherkin");
        assertThat(bySecondHint).isPresent();
        assertThat(bySecondHint.get().systemName()).isEqualTo("custom_auto");
        assertThat(requestCount.get()).isEqualTo(1);
    }

    @Test
    void resolveField_returnsEmpty_whenNothingMatchesHints() {
        handler = exchange -> respondJson(exchange, 200,
                "[{\"system_name\":\"other\",\"label\":\"Other\",\"type_id\":1}]");

        assertThat(client.resolveField("nope")).isEmpty();
    }

    @Test
    void resolveRequiredField_throws_listingAvailableFields_whenNoneMatch() {
        handler = exchange -> respondJson(exchange, 200,
                "[{\"system_name\":\"custom_x\",\"label\":\"X field\",\"type_id\":1}]");

        assertThatThrownBy(() -> client.resolveRequiredField("nope"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("custom_x")
                .hasMessageContaining("X field");
    }

    @Test
    void createCase_postsExpectedBody_andReturnsCreatedId() {
        String[] capturedBody = new String[1];
        handler = exchange -> {
            capturedBody[0] = readBody(exchange);
            respondJson(exchange, 200, "{\"id\": 321}");
        };

        long caseId = client.createCase(10, "My Scenario", 42, "JIRA-1, JIRA-2", Map.of("custom_gherkin", "body text"));

        assertThat(caseId).isEqualTo(321L);
        assertThat(capturedBody[0])
                .contains("\"title\":\"My Scenario\"")
                .contains("\"template_id\":42")
                .contains("\"refs\":\"JIRA-1, JIRA-2\"")
                .contains("\"custom_gherkin\":\"body text\"");
    }

    @Test
    void createCase_omitsRefsField_whenRefsIsBlank() {
        String[] capturedBody = new String[1];
        handler = exchange -> {
            capturedBody[0] = readBody(exchange);
            respondJson(exchange, 200, "{\"id\": 5}");
        };

        client.createCase(10, "Scenario", 42, "  ", Map.of());

        assertThat(capturedBody[0]).doesNotContain("refs");
    }

    @Test
    void resolveOrCreateSubsection_returnsExisting_whenMatchingSectionFound() {
        handler = exchange -> {
            String query = exchange.getRequestURI().getQuery();
            if (query.contains("get_sections")) {
                respondJson(exchange, 200, "{\"sections\":[{\"id\":88,\"name\":\"Login\",\"parent_id\":5}]}");
            } else {
                throw new AssertionError("add_section should not be called when a section already exists");
            }
        };

        int sectionId = client.resolveOrCreateSubsection(7, 100, 5, "Login");

        assertThat(sectionId).isEqualTo(88);
    }

    @Test
    void resolveOrCreateSubsection_ignoresSameNamedSection_underDifferentParent() {
        handler = exchange -> {
            String query = exchange.getRequestURI().getQuery();
            if (query.contains("get_sections")) {
                respondJson(exchange, 200, "{\"sections\":[{\"id\":88,\"name\":\"Login\",\"parent_id\":999}]}");
            } else if (query.contains("add_section")) {
                respondJson(exchange, 200, "{\"id\": 99}");
            }
        };

        int sectionId = client.resolveOrCreateSubsection(7, 100, 5, "Login");

        assertThat(sectionId).isEqualTo(99);
    }

    @Test
    void resolveOrCreateSubsection_createsSection_whenNoneMatches() {
        handler = exchange -> {
            String query = exchange.getRequestURI().getQuery();
            if (query.contains("get_sections")) {
                respondJson(exchange, 200, "{\"sections\":[]}");
            } else if (query.contains("add_section")) {
                respondJson(exchange, 200, "{\"id\": 99}");
            }
        };

        int sectionId = client.resolveOrCreateSubsection(7, 100, 5, "Login");

        assertThat(sectionId).isEqualTo(99);
    }

    @Test
    void getCase_returnsCaseFieldsAsMap() {
        handler = exchange -> respondJson(exchange, 200, "{\"id\":42,\"title\":\"Some case\"}");

        Map<String, Object> result = client.getCase(42);

        assertThat(result).containsEntry("title", "Some case");
    }
}
