/*
 * TESTED
 *   - Without rules: method, path, query, headers (Host included), body, status and response headers pass through;
 *     several requests in a row from one client; an Upgrade request is tunnelled both ways.
 *   - Keep-alive: one client connection carries several requests, an answered request leaves it usable for the
 *     next, chunked request and response bodies pass through, and a server that closed its kept connection gets
 *     the next request on a new one.
 *   - Respond: answers in place of the server; the path matches the end of the request path, * is one segment,
 *     the method must match; a rule with a count stops after its uses.
 *   - addRuleFirst: the newer rule answers in place of an earlier one on the same request.
 *   - DropResponse: the server handles the request, the client gets no response.
 *   - Refuse: the client fails and the server never sees the request, an Upgrade request included.
 *   - Delay: the request is held, then forwarded.
 *   - clearRules / clearTraffic, and the traffic log's outcomes, including a server that is down.
 * NOT TESTED
 *   - A real device's traffic through the proxy.
 * WHY NOT TESTED
 *   - It needs an emulator or simulator; the suites that start the proxy cover it.
 */
package com.dnestr.base.tools.faultproxy;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.dnestr.base.tools.faultproxy.ProxiedRequest.Outcome;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FaultProxyTest {

    private final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final List<String> seenByServer = new CopyOnWriteArrayList<>();
    private HttpServer server;
    private FaultProxy proxy;

    @BeforeEach
    public void startServerAndProxy() throws IOException {
        seenByServer.clear();
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            seenByServer.add("%s %s host=%s test=%s body=%s".formatted(exchange.getRequestMethod(), exchange.getRequestURI(),
                    exchange.getRequestHeaders().getFirst("Host"), exchange.getRequestHeaders().getFirst("X-Test"), body));
            byte[] response = ("echo:" + body).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("X-Upstream", "yes");
            boolean chunked = exchange.getRequestURI().getPath().endsWith("/chunked");
            exchange.sendResponseHeaders(201, chunked ? 0 : response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        proxy = FaultProxy.start(loopback(0), loopback(server.getAddress().getPort()));
    }

    @AfterEach
    public void stopServerAndProxy() throws IOException {
        proxy.close();
        server.stop(0);
    }

    @Test
    public void forwardsTheRequestAndTheResponseUntouched() throws Exception {
        HttpResponse<String> response = send(post("/api/v1/account?page=2", "hello").header("X-Test", "kept").build());

        assertEquals(201, response.statusCode());
        assertEquals("echo:hello", response.body());
        assertEquals("yes", response.headers().firstValue("X-Upstream").orElse(null));
        assertEquals(List.of("POST /api/v1/account?page=2 host=127.0.0.1:%d test=kept body=hello".formatted(proxy.port())), seenByServer);
        assertEquals(List.of("POST /api/v1/account FORWARDED"), outcomes());
    }

    @Test
    public void forwardsSeveralRequestsInARowFromOneClient() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertEquals(201, send(get("/api/v1/account")).statusCode());
        }
        assertEquals(3, seenByServer.size());
    }

    @Test
    public void oneClientConnectionCarriesSeveralRequests() throws Exception {
        try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), proxy.port())) {
            socket.setSoTimeout(5_000);
            for (int i = 0; i < 3; i++) {
                assertEquals("201 echo:n" + i, exchangeOn(socket, "POST", "/api/v1/account/state", "n" + i));
            }
        }
        assertEquals(3, seenByServer.size());
        assertEquals(List.of("POST /api/v1/account/state FORWARDED", "POST /api/v1/account/state FORWARDED",
                "POST /api/v1/account/state FORWARDED"), outcomes());
    }

    @Test
    public void anAnsweredRequestLeavesTheConnectionUsableForTheNext() throws Exception {
        proxy.addRule(FaultRule.on("GET", "/config.json", new Fault.Respond(200, "{\"on\":true}"), 1));

        try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), proxy.port())) {
            socket.setSoTimeout(5_000);
            assertEquals("200 {\"on\":true}", exchangeOn(socket, "GET", "/remote/config.json", null));
            assertEquals("201 echo:next", exchangeOn(socket, "POST", "/api/v1/account", "next"));
        }
        assertEquals(List.of("GET /remote/config.json RESPONDED", "POST /api/v1/account FORWARDED"), outcomes());
    }

    @Test
    public void chunkedRequestAndResponseBodiesPassThrough() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri("/api/v1/chunked"))
                .POST(HttpRequest.BodyPublishers.ofInputStream(() -> new java.io.ByteArrayInputStream("streamed".getBytes(StandardCharsets.UTF_8))))
                .build();

        HttpResponse<String> first = send(request);
        HttpResponse<String> second = send(get("/api/v1/account"));

        assertEquals(201, first.statusCode());
        assertEquals("echo:streamed", first.body());
        assertEquals("echo:", second.body());
        assertEquals("POST /api/v1/chunked host=127.0.0.1:%d test=null body=streamed".formatted(proxy.port()), seenByServer.getFirst());
    }

    @Test
    public void aServerThatClosedItsKeptConnectionGetsTheNextRequestOnANewOne() throws Exception {
        try (ServerSocket oneShot = new ServerSocket(0, 10, InetAddress.getLoopbackAddress());
             FaultProxy oneShotProxy = FaultProxy.start(loopback(0), loopback(oneShot.getLocalPort()))) {
            List<String> served = new CopyOnWriteArrayList<>();
            Thread.ofVirtual().start(() -> answerOneRequestPerConnection(oneShot, served));

            try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), oneShotProxy.port())) {
                socket.setSoTimeout(5_000);
                assertEquals("200 ok", exchangeOn(socket, "GET", "/api/v1/account", null));
                assertEquals("200 ok", exchangeOn(socket, "POST", "/api/v1/account/state", "x"));
            }
            assertEquals(List.of("GET /api/v1/account", "POST /api/v1/account/state"), served);
        }
    }

    @Test
    public void respondAnswersMatchingRequestsInPlaceOfTheServer() throws Exception {
        proxy.addRule(FaultRule.on("GET", "/account", new Fault.Respond(503, "{\"error\":\"down\"}"), FaultRule.ALWAYS));

        HttpResponse<String> first = send(get("/api/v1/account"));
        HttpResponse<String> second = send(get("/api/v1/account?version=5"));

        assertEquals(503, first.statusCode());
        assertEquals("{\"error\":\"down\"}", first.body());
        assertEquals("application/json", first.headers().firstValue("Content-Type").orElse(null));
        assertEquals(503, second.statusCode());
        assertTrue(seenByServer.isEmpty(), "the server should not see answered requests: " + seenByServer);
        assertEquals(List.of("GET /api/v1/account RESPONDED", "GET /api/v1/account RESPONDED"), outcomes());
    }

    @Test
    public void respondLeavesOtherPathsAndMethodsAlone() throws Exception {
        proxy.addRule(FaultRule.on("GET", "/account", new Fault.Respond(503, "{}"), FaultRule.ALWAYS));

        assertEquals(201, send(get("/api/v1/account/stats")).statusCode());
        assertEquals(201, send(get("/api/v1/subaccount")).statusCode());
        assertEquals(201, send(post("/api/v1/account", "x").build()).statusCode());
        assertEquals(3, seenByServer.size());
    }

    @Test
    public void aStarMatchesOnePathSegment() throws Exception {
        proxy.addRule(FaultRule.on("POST", "/rides/*/accept", new Fault.Respond(422, "{}"), FaultRule.ALWAYS));

        assertEquals(422, send(post("/api/v1/rides/42/accept", "").build()).statusCode());
        assertEquals(201, send(post("/api/v1/rides/42/7/accept", "").build()).statusCode());
        assertEquals(201, send(post("/api/v1/rides/accept", "").build()).statusCode());
    }

    @Test
    public void aRuleWithACountStopsAfterItsUses() throws Exception {
        proxy.addRule(FaultRule.on(null, "/account", new Fault.Respond(500, "{}"), 2));

        assertEquals(500, send(get("/api/v1/account")).statusCode());
        assertEquals(500, send(post("/api/v1/account", "").build()).statusCode());
        assertEquals(201, send(get("/api/v1/account")).statusCode());
    }

    @Test
    public void aRuleAddedFirstReplacesWhatAnEarlierRuleAnswered() throws Exception {
        proxy.addRule(FaultRule.on("GET", "/config.json", new Fault.Respond(200, "old"), FaultRule.ALWAYS));
        assertEquals("old", send(get("/remote/config.json")).body());

        proxy.addRuleFirst(FaultRule.on("GET", "/config.json", new Fault.Respond(200, "new"), FaultRule.ALWAYS));

        assertEquals("new", send(get("/remote/config.json")).body());
        assertEquals("new", send(get("/remote/config.json")).body());
    }

    @Test
    public void dropResponseLetsTheServerHandleTheRequestButTheClientGetsNoResponse() {
        proxy.addRule(FaultRule.on("POST", "/rides/*/update-status", new Fault.DropResponse(), 1));

        assertThrows(IOException.class, () -> send(post("/api/v1/rides/7/update-status", "{\"status\":\"finish\"}").build()));

        assertEquals(List.of("POST /api/v1/rides/7/update-status host=127.0.0.1:%d test=null body={\"status\":\"finish\"}"
                .formatted(proxy.port())), seenByServer);
        assertEquals(List.of("POST /api/v1/rides/7/update-status RESPONSE_DROPPED"), outcomes());
    }

    @Test
    public void refuseFailsTheRequestBeforeTheServerSeesIt() throws Exception {
        proxy.addRule(FaultRule.on("POST", "/rides/*/accept", new Fault.Refuse(), 1));

        assertThrows(IOException.class, () -> send(post("/api/v1/rides/7/accept", "").build()));
        assertTrue(seenByServer.isEmpty(), "the server should not see a refused request: " + seenByServer);

        assertEquals(201, send(post("/api/v1/rides/7/accept", "").build()).statusCode());
        assertEquals(List.of("POST /api/v1/rides/7/accept REFUSED", "POST /api/v1/rides/7/accept FORWARDED"), outcomes());
    }

    @Test
    public void delayHoldsTheRequestThenForwardsIt() throws Exception {
        proxy.addRule(FaultRule.on("POST", "/accept", new Fault.Delay(Duration.ofMillis(700)), FaultRule.ALWAYS));

        long start = System.nanoTime();
        HttpResponse<String> response = send(post("/api/v1/rides/7/accept", "late").build());

        assertTrue(Duration.ofNanos(System.nanoTime() - start).toMillis() >= 700, "the request should have been held");
        assertEquals("echo:late", response.body());
        assertEquals(1, seenByServer.size());
    }

    @Test
    public void clearRulesGoesBackToForwardingEverything() throws Exception {
        proxy.addRule(FaultRule.on(null, "/account", new Fault.Refuse(), FaultRule.ALWAYS));
        proxy.clearRules();

        assertEquals(201, send(get("/api/v1/account")).statusCode());
    }

    @Test
    public void clearTrafficForgetsWhatWasSeen() throws Exception {
        send(get("/api/v1/account"));
        proxy.clearTraffic();

        assertTrue(proxy.traffic().isEmpty());
    }

    @Test
    public void aServerThatIsDownFailsTheRequest() throws Exception {
        int closedPort;
        try (ServerSocket probe = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            closedPort = probe.getLocalPort();
        }
        try (FaultProxy toNowhere = FaultProxy.start(loopback(0), loopback(closedPort))) {
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:%d/api/v1/account".formatted(toNowhere.port())))
                    .POST(HttpRequest.BodyPublishers.noBody()).build();

            assertThrows(IOException.class, () -> client.send(request, HttpResponse.BodyHandlers.ofString()));
            assertEquals(Outcome.UPSTREAM_DOWN, toNowhere.traffic().getFirst().outcome());
        }
    }

    @Test
    public void anUpgradeRequestIsTunnelledBothWays() throws Exception {
        try (ServerSocket socketServer = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
             FaultProxy socketProxy = FaultProxy.start(loopback(0), loopback(socketServer.getLocalPort()))) {
            Thread.ofVirtual().start(() -> acceptOneWebsocket(socketServer));

            try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), socketProxy.port())) {
                socket.setSoTimeout(5_000);
                OutputStream out = socket.getOutputStream();
                out.write(("GET /socket.io/?EIO=4&transport=websocket HTTP/1.1\r\nHost: x\r\n"
                        + "Connection: Upgrade\r\nUpgrade: websocket\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
                InputStream in = socket.getInputStream();
                assertTrue(readHead(in).startsWith("HTTP/1.1 101"), "the server's 101 should reach the client");

                out.write("ping".getBytes(StandardCharsets.US_ASCII));
                assertEquals("ping", new String(in.readNBytes(4), StandardCharsets.US_ASCII));
            }
        }
    }

    /** Answers without Connection: close and then closes anyway, as a server dropping an idle kept connection does. */
    @Test
    public void refuseAppliesToAnUpgradeRequestToo() throws Exception {
        try (ServerSocket socketServer = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
             FaultProxy socketProxy = FaultProxy.start(loopback(0), loopback(socketServer.getLocalPort()))) {
            socketProxy.addRule(FaultRule.on(null, "/socket.io/", new Fault.Refuse(), FaultRule.ALWAYS));
            socketServer.setSoTimeout(500);

            try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), socketProxy.port())) {
                socket.setSoTimeout(5_000);
                socket.getOutputStream().write(("GET /socket.io/?EIO=4&transport=websocket HTTP/1.1\r\nHost: x\r\n"
                        + "Connection: Upgrade\r\nUpgrade: websocket\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
                assertThrows(IOException.class, () -> {
                    if (socket.getInputStream().read() == -1) throw new IOException("closed");
                });
            }
            assertThrows(java.net.SocketTimeoutException.class, socketServer::accept);
            assertEquals(Outcome.REFUSED, socketProxy.traffic().getFirst().outcome());
        }
    }

    private static void answerOneRequestPerConnection(ServerSocket serverSocket, List<String> served) {
        while (!serverSocket.isClosed()) {
            try (Socket socket = serverSocket.accept()) {
                InputStream in = socket.getInputStream();
                String head = readHead(in);
                String length = head.lines().filter(l -> l.toLowerCase().startsWith("content-length:")).findFirst()
                        .map(l -> l.substring(l.indexOf(':') + 1).trim()).orElse("0");
                in.readNBytes(Integer.parseInt(length));
                served.add(head.lines().findFirst().orElseThrow().replace(" HTTP/1.1", ""));
                socket.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Length: 2\r\n\r\nok".getBytes(StandardCharsets.US_ASCII));
            } catch (IOException ignored) {
                // Closed when the test ends.
            }
        }
    }

    /** One request and its response on a connection the caller keeps; returns "status body". */
    private static String exchangeOn(Socket socket, String method, String path, String body) throws IOException {
        byte[] payload = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
        String head = "%s %s HTTP/1.1\r\nHost: x\r\n%s\r\n".formatted(method, path,
                body == null ? "" : "Content-Length: " + payload.length + "\r\n");
        OutputStream out = socket.getOutputStream();
        out.write(head.getBytes(StandardCharsets.US_ASCII));
        out.write(payload);
        out.flush();
        InputStream in = socket.getInputStream();
        String responseHead = readHead(in);
        int length = responseHead.lines().filter(l -> l.toLowerCase().startsWith("content-length:")).findFirst()
                .map(l -> Integer.parseInt(l.substring(l.indexOf(':') + 1).trim())).orElseThrow();
        String status = responseHead.split(" ")[1];
        return status + " " + new String(in.readNBytes(length), StandardCharsets.UTF_8);
    }

    private static void acceptOneWebsocket(ServerSocket socketServer) {
        try (Socket socket = socketServer.accept()) {
            InputStream in = socket.getInputStream();
            String head = readHead(in);
            if (!head.contains("Connection: Upgrade")) throw new IllegalStateException("Upgrade header lost: " + head);
            OutputStream out = socket.getOutputStream();
            out.write("HTTP/1.1 101 Switching Protocols\r\nConnection: Upgrade\r\nUpgrade: websocket\r\n\r\n"
                    .getBytes(StandardCharsets.US_ASCII));
            out.write(in.readNBytes(4));
            out.flush();
        } catch (IOException ignored) {
            // The client side asserts.
        }
    }

    private static String readHead(InputStream in) throws IOException {
        ByteArrayOutputStream head = new ByteArrayOutputStream();
        while (!head.toString(StandardCharsets.US_ASCII).endsWith("\r\n\r\n")) {
            int b = in.read();
            if (b == -1) break;
            head.write(b);
        }
        return head.toString(StandardCharsets.US_ASCII);
    }

    private List<String> outcomes() {
        return proxy.traffic().stream().map(r -> r.method() + " " + r.path() + " " + r.outcome()).toList();
    }

    private HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest get(String path) {
        return HttpRequest.newBuilder(uri(path)).GET().build();
    }

    private HttpRequest.Builder post(String path, String body) {
        return HttpRequest.newBuilder(uri(path)).POST(HttpRequest.BodyPublishers.ofString(body));
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:%d%s".formatted(proxy.port(), path));
    }

    private static InetSocketAddress loopback(int port) {
        return new InetSocketAddress(InetAddress.getLoopbackAddress(), port);
    }
}
