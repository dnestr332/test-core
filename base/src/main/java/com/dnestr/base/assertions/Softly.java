package com.dnestr.base.assertions;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.data.Offset;

/**
 * Framework-agnostic soft assertion entrypoint: unlike {@link Hardly}, a failing check here is
 * recorded rather than thrown immediately, so a scenario can report every failure found in one run
 * instead of stopping at the first. Failures accumulate on a {@link ThreadLocal}
 * {@link SoftAssertions} instance (one active batch per thread), so this is safe to call from
 * parallel scenario execution as long as each scenario runs on its own thread. A batch must be
 * closed out with {@link #assertAll()} (which throws if anything failed) or {@link #reset()}
 * (which discards the batch silently); forgetting to do either leaks accumulated failures into
 * whatever test runs next on the same (possibly reused) thread.
 */
public final class Softly {

    private Softly() {}

    private static final ThreadLocal<SoftAssertions> SOFTLY = ThreadLocal.withInitial(SoftAssertions::new);

    /** Returns the current thread's accumulating {@link SoftAssertions} batch (created on first access). */
    public static SoftAssertions getSoftly() {
        return SOFTLY.get();
    }

    /** Records a failure (does not throw) if {@code condition} is not {@code true}; {@code context} is prefixed to the eventual failure message. */
    public static void isTrue(boolean condition, String context) {
        getSoftly().assertThat(condition)
                .as("%s | Condition: <%s> should be true: ", context, condition)
                .isTrue();
    }

    /** Records a failure (does not throw) if {@code condition} is not {@code false}; {@code context} is prefixed to the eventual failure message. */
    public static void isFalse(boolean condition, String context) {
        getSoftly().assertThat(condition)
                .as("%s | Condition: <%s> should be false", context, condition)
                .isFalse();
    }

    /** Records a failure (does not throw) if {@code actual} doesn't equal {@code expected}; {@code context} is prefixed to the eventual failure message. */
    public static <T> void isEqual(T actual, T expected, String context) {
        getSoftly().assertThat(actual)
                .as("%s | The actual: <%s> is equal to: <%s>", context, actual, expected)
                .isEqualTo(expected);
    }

    /** Records a failure (does not throw) if {@code actual} isn't within {@code offset} of {@code expected}; {@code context} is prefixed to the eventual failure message. */
    public static void isDoubleEqual(double actual, double expected, double offset, String context) {
        getSoftly().assertThat(actual)
                .as("%s | The actual: <%s> is equal to: <%s>", context, actual, expected)
                .isCloseTo(expected, Offset.offset(offset));
    }

    /** Records an unconditional failure (does not throw) with {@code errorMessage}. */
    public static void fail(String errorMessage) {
        getSoftly().fail(errorMessage);
    }

    /**
     * Closes out the current thread's batch: throws a single combined {@link AssertionError}
     * listing every recorded failure if there was at least one, otherwise does nothing. Either way,
     * clears the thread's batch afterward so the next call to {@link #getSoftly()} starts fresh.
     */
    public static void assertAll() {
        try {
            getSoftly().assertAll();
        } finally {
            SOFTLY.remove();
        }
    }

    /**
     * Clears any accumulated soft-assertion state for the current thread without asserting it.
     * Call this defensively (e.g. in an {@code @AfterEach}) so that a test which throws before
     * reaching {@link #assertAll()} cannot leak stale failures into the next test run on a
     * reused thread.
     */
    public static void reset() {
        SOFTLY.remove();
    }
}
