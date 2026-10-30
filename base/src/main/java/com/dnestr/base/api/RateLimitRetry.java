package com.dnestr.base.api;

import com.dnestr.base.utils.WaitUtils;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Re-sends a REST-assured call answered with {@code 429 Too Many Requests}, waiting between
 * attempts, so a transient rate limit doesn't fail the test outright. The wait is the server's
 * {@code Retry-After} header when it gives one in seconds, otherwise a linear back-off of
 * {@code baseDelay × attempt}. Once {@code maxRetries} is used up, the last 429 response is
 * returned as-is for the caller's status check to fail on.
 * <p>
 * The {@code call} is re-run in full on each attempt, so every filter on its spec (logging,
 * report attachments) sees every attempt:
 * <pre>{@code
 * Response response = RateLimitRetry.onRateLimit(() -> adminApiSpec.getSpecification(jwt)
 *         .body(body)
 *         .post(RIDES));
 * }</pre>
 * This is a call wrapper rather than a REST-assured filter because a filter can't re-send a
 * request: REST-assured's filter chain can only be walked once per request.
 */
@Slf4j
public final class RateLimitRetry {

    private static final int TOO_MANY_REQUESTS = 429;

    private RateLimitRetry() {}

    /** {@link #onRateLimit(Supplier, int, Duration)} with up to 5 retries and a 2s × attempt back-off. */
    public static Response onRateLimit(Supplier<Response> call) {
        return onRateLimit(call, 5, Duration.ofSeconds(2));
    }

    /**
     * Runs {@code call}, re-running it up to {@code maxRetries} times while it answers 429, and
     * returns the first non-429 response (or the last 429 once retries are used up).
     *
     * @throws IllegalArgumentException if {@code maxRetries} is negative
     */
    public static Response onRateLimit(Supplier<Response> call, int maxRetries, Duration baseDelay) {
        return onRateLimit(call, maxRetries, baseDelay, delay -> WaitUtils.sleepMillis(delay.toMillis()));
    }

    /** Test seam: lets tests record the waits instead of sleeping. */
    static Response onRateLimit(Supplier<Response> call, int maxRetries, Duration baseDelay, Consumer<Duration> sleeper) {
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries must not be negative, was " + maxRetries);
        }
        Response response = call.get();

        for (int attempt = 1; response.statusCode() == TOO_MANY_REQUESTS && attempt <= maxRetries; attempt++) {
            Duration delay = retryAfter(response).orElse(baseDelay.multipliedBy(attempt));
            log.warn("⚠ Rate limited (429), retrying in {} ms (attempt {}/{})", delay.toMillis(), attempt, maxRetries);
            sleeper.accept(delay);
            response = call.get();
        }
        return response;
    }

    /** The {@code Retry-After} header as a delay, if present in its delay-seconds form; the HTTP-date form is ignored in favor of the back-off. */
    private static Optional<Duration> retryAfter(Response response) {
        String header = response.getHeader("Retry-After");
        if (header == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Duration.ofSeconds(Long.parseLong(header.trim())));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
