package com.dnestr.base.tools.faultproxy;

import lombok.extern.slf4j.Slf4j;
import com.dnestr.base.tools.faultproxy.ProxiedRequest.Outcome;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * An HTTP/1.1 proxy in front of one server that forwards bytes untouched, except for the requests a
 * {@link FaultRule} matches. Connections are kept alive on both sides, since every new connection from the emulator
 * goes through its NAT; a fault applies to the one request it matched, and upgrade requests (websockets) are tunnelled.
 */
@Slf4j
public final class FaultProxy implements AutoCloseable {

    private static final int MAX_HEAD_BYTES = 64 * 1024;
    private static final int CONNECT_TIMEOUT_MILLIS = 5_000;
    private static final int UPSTREAM_READ_TIMEOUT_MILLIS = 120_000;
    private static final int DRAIN_TIMEOUT_MILLIS = 1_000;
    private static final byte[] HEAD_END = {'\r', '\n', '\r', '\n'};

    private final ServerSocket server;
    private final InetSocketAddress upstream;
    private final List<FaultRule> rules = new CopyOnWriteArrayList<>();
    private final List<ProxiedRequest> traffic = new CopyOnWriteArrayList<>();
    private volatile boolean running = true;

    private FaultProxy(ServerSocket server, InetSocketAddress upstream) {
        this.server = server;
        this.upstream = upstream;
    }

    /** Port 0 picks a free port; {@link #port()} says which. */
    public static FaultProxy start(InetSocketAddress listen, InetSocketAddress upstream) throws IOException {
        ServerSocket server = new ServerSocket();
        server.setReuseAddress(true);
        try {
            server.bind(listen, 200);
        } catch (IOException e) {
            server.close();
            throw e;
        }
        FaultProxy proxy = new FaultProxy(server, upstream);
        Thread.ofVirtual().name("fault-proxy-accept").start(proxy::acceptLoop);
        return proxy;
    }

    /** The port the proxy listens on. */
    public int port() {
        return server.getLocalPort();
    }

    /** Adds a rule after the existing ones; the first rule that matches a request applies. */
    public void addRule(FaultRule rule) {
        rules.add(rule);
        log.info("Fault proxy rule added: {}", rule);
    }

    /** Ahead of every earlier rule, so it replaces what an earlier rule on the same request answered. */
    public void addRuleFirst(FaultRule rule) {
        rules.addFirst(rule);
        log.info("Fault proxy rule added first: {}", rule);
    }

    /** Removes every rule, so every request is forwarded untouched. */
    public void clearRules() {
        rules.clear();
    }

    /** A copy of the requests seen since the last {@link #clearTraffic}, oldest first. */
    public List<ProxiedRequest> traffic() {
        return List.copyOf(traffic);
    }

    /** Forgets the requests seen so far. */
    public void clearTraffic() {
        traffic.clear();
    }

    @Override
    public void close() throws IOException {
        running = false;
        server.close();
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket client = server.accept();
                Thread.ofVirtual().start(() -> handle(client));
            } catch (IOException e) {
                if (running) log.warn("Fault proxy could not accept a connection", e);
            }
        }
    }

    private void handle(Socket client) {
        try (client; Upstream link = new Upstream()) {
            InputStream in = new BufferedInputStream(client.getInputStream());
            boolean keepOpen = true;
            while (keepOpen) {
                byte[] rawHead = readHead(in);
                if (rawHead == null) return;
                RequestHead head = RequestHead.parse(rawHead);
                byte[] body = head.upgrade ? new byte[0] : readMessageBody(in, head.headers, false);
                FaultRule rule = rules.stream().filter(r -> r.matches(head.method, head.path) && r.claim()).findFirst().orElse(null);
                Fault fault = rule == null ? null : rule.fault();
                if (head.upgrade && (fault == null || fault instanceof Fault.Delay)) {
                    if (fault instanceof Fault.Delay(Duration duration)) Thread.sleep(duration);
                    tunnel(client, in, head);
                    return;
                }
                keepOpen = switch (fault) {
                    case Fault.Respond respond -> respond(client, head, respond);
                    case Fault.Refuse ignored -> refuse(client, head);
                    case Fault.Delay delay -> {
                        Thread.sleep(delay.duration());
                        yield forward(client, link, head, body, false);
                    }
                    case Fault.DropResponse ignored -> forward(client, link, head, body, true);
                    case null -> forward(client, link, head, body, false);
                } && !head.upgrade;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            log.debug("Fault proxy connection ended: {}", e.getMessage());
        }
    }

    /** Returns whether the client connection can carry another request. */
    private boolean forward(Socket client, Upstream link, RequestHead head, byte[] body, boolean dropResponse) throws IOException {
        if (!link.connect()) {
            record(head, Outcome.UPSTREAM_DOWN);
            reset(client);
            return false;
        }
        record(head, dropResponse ? Outcome.RESPONSE_DROPPED : Outcome.FORWARDED);
        Response response = link.exchange(head, body);
        if (dropResponse) {
            reset(client);
            return false;
        }
        OutputStream out = client.getOutputStream();
        out.write(response.bytes);
        out.flush();
        return response.reusable && !head.close;
    }

    /** Hands the head on and pumps both ways until either side closes. */
    private void tunnel(Socket client, InputStream clientIn, RequestHead head) throws IOException {
        try (Socket server = new Socket()) {
            try {
                server.connect(upstream, CONNECT_TIMEOUT_MILLIS);
            } catch (IOException e) {
                record(head, Outcome.UPSTREAM_DOWN);
                reset(client);
                return;
            }
            record(head, Outcome.FORWARDED);
            OutputStream serverOut = server.getOutputStream();
            serverOut.write(head.raw);
            serverOut.flush();
            Thread.ofVirtual().start(() -> pump(clientIn, server));
            server.getInputStream().transferTo(client.getOutputStream());
        }
    }

    private static void pump(InputStream clientIn, Socket server) {
        try {
            clientIn.transferTo(server.getOutputStream());
            server.shutdownOutput();
        } catch (IOException ignored) {
            // Either side closing ends the exchange; the response pump sees it too.
        }
    }

    private boolean respond(Socket client, RequestHead head, Fault.Respond respond) throws IOException {
        record(head, Outcome.RESPONDED);
        byte[] body = respond.body().getBytes(StandardCharsets.UTF_8);
        String headers = "HTTP/1.1 %d %s\r\nContent-Type: application/json\r\nContent-Length: %d\r\n%s\r\n"
                .formatted(respond.status(), reason(respond.status()), body.length, head.close ? "Connection: close\r\n" : "");
        OutputStream out = client.getOutputStream();
        out.write(headers.getBytes(StandardCharsets.US_ASCII));
        out.write(body);
        out.flush();
        if (!head.close) return true;
        client.shutdownOutput();
        drain(client);
        return false;
    }

    private boolean refuse(Socket client, RequestHead head) throws SocketException {
        record(head, Outcome.REFUSED);
        reset(client);
        return false;
    }

    private void record(RequestHead head, Outcome outcome) {
        traffic.add(new ProxiedRequest(Instant.now(), head.method, head.path, outcome));
    }

    /** A reset, not a clean close, so the client fails the request as a network error. */
    private static void reset(Socket client) throws SocketException {
        client.setSoLinger(true, 0);
    }

    private static void drain(Socket client) {
        try {
            client.setSoTimeout(DRAIN_TIMEOUT_MILLIS);
            client.getInputStream().transferTo(OutputStream.nullOutputStream());
        } catch (SocketTimeoutException ignored) {
            // The client kept the connection open; closing it now is fine.
        } catch (IOException ignored) {
            // Already closed by the client.
        }
    }

    /** Reads up to the blank line ending the head; null when the connection closes before a new one starts. */
    private static byte[] readHead(InputStream in) throws IOException {
        ByteArrayOutputStream head = new ByteArrayOutputStream();
        int matched = 0;
        while (matched < HEAD_END.length) {
            int b = in.read();
            if (b == -1) {
                if (head.size() == 0) return null;
                throw new IOException("Connection closed inside a message head");
            }
            head.write(b);
            matched = b == HEAD_END[matched] ? matched + 1 : (b == HEAD_END[0] ? 1 : 0);
            if (head.size() > MAX_HEAD_BYTES) throw new IOException("Message head over " + MAX_HEAD_BYTES + " bytes");
        }
        return head.toByteArray();
    }

    /**
     * The body as it was framed on the wire (chunked bodies keep their chunks). Without a length or chunking a
     * request has no body, while a response runs to the end of the connection.
     */
    private static byte[] readMessageBody(InputStream in, List<String> headers, boolean untilClose) throws IOException {
        if (headerValue(headers, "transfer-encoding").toLowerCase(Locale.ROOT).contains("chunked")) return readChunked(in);
        String length = headerValue(headers, "content-length");
        if (!length.isEmpty()) {
            int size = Integer.parseInt(length.trim());
            byte[] body = in.readNBytes(size);
            if (body.length < size) throw new IOException("Connection closed inside a body");
            return body;
        }
        return untilClose ? in.readAllBytes() : new byte[0];
    }

    private static byte[] readChunked(InputStream in) throws IOException {
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        while (true) {
            String sizeLine = readLine(in, raw);
            int size = Integer.parseInt(sizeLine.split(";", 2)[0].trim(), 16);
            if (size == 0) {
                String trailer;
                do {
                    trailer = readLine(in, raw);
                } while (!trailer.isEmpty());
                return raw.toByteArray();
            }
            byte[] chunk = in.readNBytes(size + 2);
            if (chunk.length < size + 2) throw new IOException("Connection closed inside a chunk");
            raw.write(chunk);
        }
    }

    private static String readLine(InputStream in, ByteArrayOutputStream raw) throws IOException {
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        int previous = -1;
        while (true) {
            int b = in.read();
            if (b == -1) throw new IOException("Connection closed inside a chunked body");
            raw.write(b);
            if (previous == '\r' && b == '\n') {
                byte[] bytes = line.toByteArray();
                return new String(bytes, 0, bytes.length - 1, StandardCharsets.ISO_8859_1);
            }
            line.write(b);
            previous = b;
        }
    }

    private static String headerValue(List<String> headers, String name) {
        for (String header : headers) {
            int colon = header.indexOf(':');
            if (colon > 0 && header.substring(0, colon).trim().equalsIgnoreCase(name)) return header.substring(colon + 1).trim();
        }
        return "";
    }

    private static List<String> headerLines(byte[] raw) {
        String[] lines = new String(raw, StandardCharsets.ISO_8859_1).split("\r\n");
        List<String> headers = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            if (!lines[i].isEmpty()) headers.add(lines[i]);
        }
        return headers;
    }

    private static String reason(int status) {
        return switch (status) {
            case 200 -> "OK";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 422 -> "Unprocessable Entity";
            case 500 -> "Internal Server Error";
            case 502 -> "Bad Gateway";
            case 503 -> "Service Unavailable";
            case 504 -> "Gateway Timeout";
            default -> "Status";
        };
    }

    /** The response as it goes back to the client, and whether the server keeps the connection for the next request. */
    private record Response(byte[] bytes, boolean reusable) {
    }

    /** The server side of one client connection, opened on its first forwarded request and reused after that. */
    private final class Upstream implements Closeable {

        private Socket socket;
        private InputStream in;
        private boolean used;

        boolean connect() {
            if (socket != null) return true;
            Socket fresh = new Socket();
            try {
                fresh.connect(upstream, CONNECT_TIMEOUT_MILLIS);
                fresh.setSoTimeout(UPSTREAM_READ_TIMEOUT_MILLIS);
                socket = fresh;
                in = new BufferedInputStream(fresh.getInputStream());
                used = false;
                return true;
            } catch (IOException e) {
                closeQuietly(fresh);
                return false;
            }
        }

        /**
         * A kept connection the server has since closed fails before any reply, so that one is retried on a new one;
         * a reply that only timed out is not, since the server may have acted on the request.
         */
        Response exchange(RequestHead head, byte[] body) throws IOException {
            boolean reused = used;
            try {
                return send(head, body);
            } catch (SocketTimeoutException e) {
                throw e;
            } catch (IOException e) {
                if (!reused) throw e;
                drop();
                if (!connect()) throw e;
                return send(head, body);
            }
        }

        private Response send(RequestHead head, byte[] body) throws IOException {
            used = true;
            OutputStream out = socket.getOutputStream();
            out.write(head.raw);
            out.write(body);
            out.flush();
            ByteArrayOutputStream response = new ByteArrayOutputStream();
            byte[] rawHead;
            int status;
            do {
                rawHead = readHead(in);
                if (rawHead == null) throw new IOException("The server closed the connection without a response");
                response.write(rawHead);
                status = statusOf(rawHead);
            } while (status / 100 == 1);
            List<String> headers = headerLines(rawHead);
            boolean noBody = head.method.equalsIgnoreCase("HEAD") || status == 204 || status == 304;
            boolean delimited = noBody || !headerValue(headers, "content-length").isEmpty()
                    || headerValue(headers, "transfer-encoding").toLowerCase(Locale.ROOT).contains("chunked");
            if (!noBody) response.write(readMessageBody(in, headers, true));
            boolean reusable = delimited && !headerValue(headers, "connection").equalsIgnoreCase("close");
            if (!reusable) drop();
            return new Response(response.toByteArray(), reusable);
        }

        private static int statusOf(byte[] rawHead) throws IOException {
            String statusLine = new String(rawHead, StandardCharsets.ISO_8859_1).split("\r\n", 2)[0];
            String[] parts = statusLine.split(" ");
            if (parts.length < 2) throw new IOException("Bad status line: " + statusLine);
            return Integer.parseInt(parts[1]);
        }

        private void drop() {
            closeQuietly(socket);
            socket = null;
            in = null;
        }

        @Override
        public void close() {
            drop();
        }

        private static void closeQuietly(Socket socket) {
            if (socket == null) return;
            try {
                socket.close();
            } catch (IOException ignored) {
                // Nothing left to release.
            }
        }
    }

    private record RequestHead(String method, String path, byte[] raw, List<String> headers, boolean upgrade, boolean close) {

        static RequestHead parse(byte[] raw) throws IOException {
            String requestLine = new String(raw, StandardCharsets.ISO_8859_1).split("\r\n", 2)[0];
            String[] parts = requestLine.split(" ");
            if (parts.length < 3) throw new IOException("Bad request line: " + requestLine);
            String target = parts[1];
            int query = target.indexOf('?');
            List<String> headers = headerLines(raw);
            boolean upgrade = !headerValue(headers, "upgrade").isEmpty();
            boolean close = headerValue(headers, "connection").equalsIgnoreCase("close");
            return new RequestHead(parts[0], query < 0 ? target : target.substring(0, query), raw, headers, upgrade, close);
        }
    }
}
