package com.dnestr.base.utils;

import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;

import java.time.Duration;
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
public final class WaitUtils {

    private WaitUtils() {}

    private static final long DEFAULT_TIMEOUT = 10;
    private static final long DEFAULT_POLLING = 200;

    /** {@link #waitUntil(BooleanSupplier, long, long, String)} with the default 10s timeout and 200ms polling interval. */
    public static void waitUntil(BooleanSupplier condition) {
        waitUntil(condition, DEFAULT_TIMEOUT, DEFAULT_POLLING, "Condition not met");
    }

    /** {@link #waitUntil(BooleanSupplier, long, long, String)} with the default 200ms polling interval. */
    public static void waitUntil(BooleanSupplier condition, long seconds, String message) {
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
    public static void waitUntil(BooleanSupplier condition, long seconds, long pollingMillis, String message) {
        Awaitility.await()
                .pollInterval(Duration.ofMillis(pollingMillis))
                .atMost(Duration.ofSeconds(seconds))
                .until(() -> {
                    boolean result = condition.getAsBoolean();
                    if (!result) log.debug("Waiting: {}", message);
                    return result;
                });
    }

    /**
     * Like {@link #waitUntil(BooleanSupplier, long, String)}, but any exception thrown by
     * {@code condition} during a poll is swallowed and treated as "not yet true" rather than
     * failing the wait — useful when {@code condition} itself can transiently throw while the
     * state it reads is still settling (e.g. a resource that isn't created yet).
     *
     * @throws org.awaitility.core.ConditionTimeoutException if the timeout is reached
     */
    public static void waitUntilIgnoringExceptions(BooleanSupplier condition, long seconds, String message) {
        Awaitility.await()
                .ignoreExceptions()
                .pollInterval(Duration.ofMillis(DEFAULT_POLLING))
                .atMost(Duration.ofSeconds(seconds))
                .until(() -> {
                    boolean result = condition.getAsBoolean();
                    if (!result) log.debug("Waiting: {}", message);
                    return result;
                });
    }

    /**
     * Polls {@code supplier} (ignoring any exception it throws, like
     * {@link #waitUntilIgnoringExceptions}) until its value satisfies {@code condition} or
     * {@code seconds} elapses, then returns that value.
     *
     * @throws org.awaitility.core.ConditionTimeoutException if the timeout is reached
     */
    public static <T> T waitForValue(Supplier<T> supplier, Predicate<T> condition, long seconds, String message) {
        return Awaitility.await()
                .ignoreExceptions()
                .pollInterval(Duration.ofMillis(DEFAULT_POLLING))
                .atMost(Duration.ofSeconds(seconds))
                .until(() -> {
                    T value = supplier.get();
                    if (!condition.test(value)) {
                        log.debug("Waiting: {}", message);
                    }
                    return value;
                }, condition);
    }

    /** Blocks the current thread for {@code seconds}. Prefer polling ({@link #waitUntil}) over a fixed sleep wherever the condition being waited on can be expressed as one. */
    public static void sleepSeconds(long seconds) {
        sleepMillis(seconds * 1000);
    }

    /** Blocks the current thread for {@code millis}; if interrupted, re-sets the thread's interrupt flag and logs a warning rather than propagating {@link InterruptedException}. */
    public static void sleepMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Sleep interrupted", e);
        }
    }
}