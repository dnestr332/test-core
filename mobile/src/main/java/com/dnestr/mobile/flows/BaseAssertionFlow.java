package com.dnestr.mobile.flows;

import com.dnestr.mobile.assertions.Hardly;
import com.dnestr.base.assertions.Softly;
import lombok.extern.slf4j.Slf4j;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Base for flow classes that assert on mobile screen state. Unlike the web module's
 * {@code BaseAssertionFlow} (one generic {@code verify*} family dispatched by an
 * {@code AssertionState} parameter), hard and soft checks are separate named methods here
 * ({@code assert*} vs {@code softAssert*}), and every check is still a single synchronous read —
 * there is no Playwright-style auto-retrying assertion available on this platform (see
 * {@link Hardly}'s class docs) — so callers should wait for the UI to settle before asserting.
 * <p>
 * The {@code softAssert*} methods additionally catch a {@link RuntimeException} thrown while
 * reading {@code actual}/{@code condition} (e.g. a stale-element error) and treat it as a failed
 * assertion (recorded via {@code Softly}, batch not aborted) rather than letting it propagate and
 * abort the whole soft-assertion batch — the {@code assert*} (hard) methods have no such
 * protection and let such an exception propagate normally.
 */
@Slf4j
public abstract class BaseAssertionFlow {

    /** Asserts {@code condition} is {@code true}, failing the test immediately if not. */
    public void assertTrue(BooleanSupplier condition, String message) {
        Hardly.isTrue(condition.getAsBoolean(), message);
    }

    /** Asserts {@code actual.get()} equals {@code expected}, failing the test immediately if not. */
    public <T> void assertEquals(Supplier<T> actual, T expected, String message) {
        Hardly.isEqual(actual.get(), expected, message);
    }

    /** Records a failure (does not throw) if {@code actual.get()} isn't within {@code offset} of {@code expected}. */
    public void softAssertDoubleEquals(Supplier<Double> actual, double expected, double offset, String message) {
        Softly.isDoubleEqual(actual.get(), expected, offset, message);
    }

    /**
     * Records a failure (does not throw) if {@code condition} isn't {@code true}. If evaluating
     * {@code condition} itself throws a {@link RuntimeException}, that's logged and treated as a
     * failed ({@code false}) result rather than propagating.
     */
    public void softAssertTrue(BooleanSupplier condition, String message) {
        boolean result = false;
        try {
            result = condition.getAsBoolean();
        } catch (RuntimeException e) {
            logSoftAssertionException(e);
        }
        Softly.isTrue(result, message);
    }

    /**
     * Records a failure (does not throw) if {@code actual.get()} doesn't equal {@code expected}. If
     * evaluating {@code actual} itself throws a {@link RuntimeException}, that's logged and treated
     * as a {@code null} actual value rather than propagating.
     */
    public <T> void softAssertEquals(Supplier<T> actual, T expected, String message) {
        T actualValue = null;
        try {
            actualValue = actual.get();
        } catch (RuntimeException e) {
            logSoftAssertionException(e);
        }
        Softly.isEqual(actualValue, expected, message);
    }

    private void logSoftAssertionException(RuntimeException e) {
        log.warn("Exception was caught during SOFT assertion: {}", e.getMessage());
    }
}

