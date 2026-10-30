package com.dnestr.base.api;

import com.sun.net.httpserver.HttpServer;
import io.restassured.RestAssured;
import io.restassured.filter.Filter;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimitRetryTest {

    /** A status to answer with, plus an optional {@code Retry-After} value. */
    private record Reply(int status, String retryAfter) {
        static Reply of(int status) {
            return new Reply(status, null);
        }
    }

    private HttpServer server;
    private final Deque<Reply> replies = new ConcurrentLinkedDeque<>();
    private final AtomicInteger hits = new AtomicInteger();
    private final List<Duration> waits = new ArrayList<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            hits.incrementAndGet();
            Reply reply = replies.isEmpty() ? Reply.of(200) : replies.poll();
            if (reply.retryAfter() != null) {
                exchange.getResponseHeaders().add("Retry-After", reply.retryAfter());
            }
            exchange.sendResponseHeaders(reply.status(), -1);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void passesThroughNonRateLimitedResponseWithoutRetrying() {
        replies.add(Reply.of(500));

        Response response = getWithRetry(3);

        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(hits).hasValue(1);
        assertThat(waits).isEmpty();
    }

    @Test
    void retriesUntilRequestIsNoLongerRateLimited() {
        replies.add(Reply.of(429));
        replies.add(Reply.of(429));
        replies.add(Reply.of(200));

        Response response = getWithRetry(5);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(hits).hasValue(3);
    }

    @Test
    void backsOffLinearlyWithoutRetryAfterHeader() {
        replies.add(Reply.of(429));
        replies.add(Reply.of(429));
        replies.add(Reply.of(429));

        getWithRetry(5);

        assertThat(waits).containsExactly(Duration.ofMillis(100), Duration.ofMillis(200), Duration.ofMillis(300));
    }

    @Test
    void waitsForRetryAfterSecondsWhenGiven() {
        replies.add(new Reply(429, "7"));

        getWithRetry(5);

        assertThat(waits).containsExactly(Duration.ofSeconds(7));
    }

    @Test
    void fallsBackToBackOffForHttpDateRetryAfter() {
        replies.add(new Reply(429, "Wed, 21 Oct 2026 07:28:00 GMT"));

        getWithRetry(5);

        assertThat(waits).containsExactly(Duration.ofMillis(100));
    }

    @Test
    void returnsLast429OnceRetriesAreUsedUp() {
        for (int i = 0; i < 5; i++) {
            replies.add(Reply.of(429));
        }

        Response response = getWithRetry(2);

        assertThat(response.statusCode()).isEqualTo(429);
        assertThat(hits).hasValue(3);
        assertThat(waits).hasSize(2);
    }

    @Test
    void zeroRetriesDisablesRetrying() {
        replies.add(Reply.of(429));

        Response response = getWithRetry(0);

        assertThat(response.statusCode()).isEqualTo(429);
        assertThat(hits).hasValue(1);
    }

    @Test
    void rejectsNegativeMaxRetries() {
        assertThatThrownBy(() -> RateLimitRetry.onRateLimit(() -> null, -1, Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxRetries");
        assertThat(hits).hasValue(0);
    }

    @Test
    void everyAttemptRunsTheSpecFilters() {
        replies.add(Reply.of(429));
        replies.add(Reply.of(200));
        AtomicInteger filterRuns = new AtomicInteger();
        Filter countingFilter = (request, response, context) -> {
            filterRuns.incrementAndGet();
            return context.next(request, response);
        };
        BaseApiSpec spec = new BaseApiSpec() {
            @Override
            protected String getUrl() {
                return baseUri();
            }

            @Override
            protected String getPath() {
                return "/v1";
            }

            @Override
            protected List<Filter> filters() {
                return List.of(countingFilter);
            }
        };

        Response response = RateLimitRetry.onRateLimit(
                () -> spec.baseSpec().get("/rides"), 3, Duration.ofMillis(100), waits::add);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(filterRuns).hasValue(2);
    }

    private Response getWithRetry(int maxRetries) {
        return RateLimitRetry.onRateLimit(
                () -> RestAssured.given().baseUri(baseUri()).get("/rides"),
                maxRetries, Duration.ofMillis(100), waits::add);
    }

    private String baseUri() {
        return "http://localhost:" + server.getAddress().getPort();
    }
}
