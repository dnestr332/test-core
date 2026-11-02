package com.dnestr.base.assertions;

import lombok.experimental.UtilityClass;
import org.assertj.core.api.Assertions;

/**
 * Framework-agnostic hard (fail-fast) assertion entrypoint: a thin AssertJ wrapper that attaches a
 * {@code context} description to the failure message and throws on the first failing assertion.
 * Every check here evaluates its input exactly once, synchronously — there is no polling/retry, so
 * this is only appropriate once the value being asserted on is already known to be settled. For
 * auto-retrying element checks in web tests, use Playwright's own
 * {@code PlaywrightAssertions.assertThat(locator)} directly.
 */
@UtilityClass
public class Hardly {

    /** Asserts {@code condition} is {@code true}; {@code context} is prefixed to the failure message. */
    public void isTrue(boolean condition, String context) {
        Assertions.assertThat(condition)
                .as("%s | Condition: <%s> should be true: ", context, condition)
                .isTrue();
    }

    /** Asserts {@code condition} is {@code false}; {@code context} is prefixed to the failure message. */
    public void isFalse(boolean condition, String context) {
        Assertions.assertThat(condition)
                .as("%s | Condition: <%s> should be false", context, condition)
                .isFalse();
    }

    /** Asserts {@code actual} equals {@code expected}; {@code context} is prefixed to the failure message. */
    public <T> void isEqual(T actual, T expected, String context) {
        Assertions.assertThat(actual)
                .as("%s | The actual: <%s> is equal to: <%s>", context, actual, expected)
                .isEqualTo(expected);
    }

    /** Fails the test immediately with {@code errorMessage}, with no condition to evaluate. */
    public void fail(String errorMessage) {
        Assertions.fail(errorMessage);
    }
}
