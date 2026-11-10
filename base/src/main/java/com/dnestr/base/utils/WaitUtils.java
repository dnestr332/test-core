package com.dnestr.base.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Awaitility-backed polling helpers for waiting on non-Playwright/non-Appium conditions (e.g. an
 * async backend side effect, a background job, or any plain Java state) — for element-specific
 * waits, prefer the platform's own primitives (e.g. {@code ElementActions.waitForCondition}) which
 * integrate with that platform's own polling/failure reporting instead.
 */
@Slf4j
@UtilityClass
public class WaitUtils {

    private final long DEFAULT_TIMEOUT = 10;
    private final long DEFAULT_POLLING = 200;

    /** {@link #waitUntil(BooleanSupplier, long, long, String)} with the default 10s timeout and 200ms polling interval. */
    public void waitUntil(BooleanSupplier condition) {
        waitUntil(condition, DEFAULT_TIMEOUT, DEFAULT_POLLING, "Condition not met");
    }

    /** {@link #waitUntil(BooleanSupplier, long, long, String)} with the default 200ms polling interval. */
    public void waitUntil(BooleanSupplier condition, long seconds, String message) {
        waitUntil(condition, seconds, DEFAULT_POLLING, message);
    }

    /**
     * Polls {@code condition} every {@code pollingMillis} until it returns {@code true} or
     * {@code seconds} elapses, logging {@code message} at debug level on each unsuccessful poll.
     * An exception thrown by {@code condition} propagates immediately rather than being retried
     * (see {@link #waitUntilIgnoringExceptions} for that behavior).
     *
     * @throws org.awaitility.core.ConditionTimeoutException if the timeout is reached
     */
    public void waitUntil(BooleanSupplier condition, long seconds, long pollingMillis, String message) {
        polling(message, seconds, pollingMillis).until(logged(condition, message));
    }

    /**
     * Like {@link #waitUntil(BooleanSupplier, long, String)}, but any exception thrown by
     * {@code condition} during a poll is swallowed and treated as "not yet true" rather than
     * failing the wait — useful when {@code condition} itself can transiently throw while the
     * state it reads is still settling (e.g. a resource that isn't created yet).
     *
     * @throws org.awaitility.core.ConditionTimeoutException if the timeout is reached
     */
    public void waitUntilIgnoringExceptions(BooleanSupplier condition, long seconds, String message) {
        polling(message, seconds, DEFAULT_POLLING).ignoreExceptions().until(logged(condition, message));
    }

    /**
     * Polls {@code supplier} (ignoring any exception it throws, like
     * {@link #waitUntilIgnoringExceptions}) until its value satisfies {@code condition} or
     * {@code seconds} elapses, then returns that value.
     *
     * @throws org.awaitility.core.ConditionTimeoutException if the timeout is reached
     */
    public <T> T waitForValue(Supplier<T> supplier, Predicate<T> condition, long seconds, String message) {
        return polling(message, seconds, DEFAULT_POLLING)
                .ignoreExceptions()
                .until(() -> {
                    T value = supplier.get();
                    if (!condition.test(value)) {
                        logWaiting(message);
                    }
                    return value;
                }, condition);
    }

    /** {@link #waitUntilOnCallerThread(BooleanSupplier, long, long, String)} with the default 200ms polling interval. */
    public void waitUntilOnCallerThread(BooleanSupplier condition, long seconds, String message) {
        waitUntilOnCallerThread(condition, seconds, DEFAULT_POLLING, message);
    }

    /**
     * Like {@link #waitUntil(BooleanSupplier, long, long, String)}, but polls on the calling thread, so a condition
     * that reads thread-bound state (a scenario-scoped bean, a {@link ThreadLocal}) works. The timeout is checked
     * between polls, so a condition that blocks holds the wait up.
     *
     * @throws org.awaitility.core.ConditionTimeoutException if the timeout is reached
     */
    public void waitUntilOnCallerThread(BooleanSupplier condition, long seconds, long pollingMillis, String message) {
        polling(message, seconds, pollingMillis)
                .pollInSameThread()
                .until(logged(condition, message));
    }

    /** Blocks the current thread for {@code seconds}. Prefer polling ({@link #waitUntil}) over a fixed sleep wherever the condition being waited on can be expressed as one. */
    public void sleepSeconds(long seconds) {
        sleepMillis(seconds * 1000);
    }

    /** {@link #sleepSeconds(long)} that logs a warning naming why, so a fixed sleep stands out in the run log. */
    public void sleepSeconds(long seconds, String reason) {
        log.warn("Sleeping {}s: {}", seconds, reason);
        sleepSeconds(seconds);
    }

    /** Blocks the current thread for {@code millis}; if interrupted, re-sets the thread's interrupt flag and logs a warning rather than propagating {@link InterruptedException}. */
    public void sleepMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Sleep interrupted", e);
        }
    }

    /** Named after {@code message}, so a timeout says what was awaited. */
    private ConditionFactory polling(String message, long seconds, long pollingMillis) {
        return Awaitility.await(message)
                .pollInterval(Duration.ofMillis(pollingMillis))
                .atMost(Duration.ofSeconds(seconds));
    }

    private Callable<Boolean> logged(BooleanSupplier condition, String message) {
        return () -> {
            boolean result = condition.getAsBoolean();
            if (!result) logWaiting(message);
            return result;
        };
    }

    private void logWaiting(String message) {
        log.debug("Waiting: {}", message);
    }
}
