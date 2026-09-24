package com.dnestr.mobile.assertions;

/**
 * Hard (fail-fast) assertion entrypoint for mobile tests. Unlike {@code com.dnestr.web.assertions.Hardly},
 * this is a pure pass-through to {@code com.dnestr.base.assertions.Hardly} with no element-level
 * assertion methods of its own — Selenium/Appium has no equivalent to Playwright's auto-retrying
 * "web-first" assertions, so there's no locator-based {@code isVisible}/{@code hasText}/etc. here.
 * Every check is a single synchronous read (typically via {@code ElementActions}) passed to
 * {@link #isTrue}/{@link #isEqual}; callers needing retry-until-stable behavior should wait first
 * (e.g. {@code DriverWait}) rather than expect this class to poll.
 */
public final class Hardly {

    private Hardly() {}

    /** Asserts {@code condition} is {@code true}; {@code context} is prefixed to the failure message. */
    public static void isTrue(boolean condition, String context) {
        com.dnestr.base.assertions.Hardly.isTrue(condition, context);
    }

    /** Asserts {@code condition} is {@code false}; {@code context} is prefixed to the failure message. */
    public static void isFalse(boolean condition, String context) {
        com.dnestr.base.assertions.Hardly.isFalse(condition, context);
    }

    /** Asserts {@code actual} equals {@code expected}; {@code context} is prefixed to the failure message. */
    public static <T> void isEqual(T actual, T expected, String context) {
        com.dnestr.base.assertions.Hardly.isEqual(actual, expected, context);
    }

    /** Fails the test immediately with {@code errorMessage}, with no condition to evaluate. */
    public static void fail(String errorMessage) {
        com.dnestr.base.assertions.Hardly.fail(errorMessage);
    }
}
