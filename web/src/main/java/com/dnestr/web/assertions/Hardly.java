package com.dnestr.web.assertions;

import com.microsoft.playwright.Locator;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Hard (fail-fast) assertion entrypoint for web tests — the first failing assertion throws and
 * aborts the test immediately, unlike {@code com.dnestr.base.assertions.Softly}, which accumulates
 * failures for a batched report.
 * <p>
 * Two different execution models live behind this one entrypoint, and they're easy to mix up:
 * <ul>
 *   <li>{@link #isTrue}, {@link #isFalse}, {@link #isEqual} delegate to the framework-agnostic
 *       {@code com.dnestr.base.assertions.Hardly} (plain AssertJ) and check a value that's already
 *       been computed at the call site — they do <b>not</b> wait or retry. Passing a synchronous
 *       Playwright read like {@code locator.isVisible()} into {@code isTrue} checks it exactly
 *       once, at the instant it's called; it will not poll like the methods below do.</li>
 *   <li>Every {@link Locator}-based method here goes through Playwright's auto-retrying "web-first"
 *       assertions ({@code PlaywrightAssertions.assertThat(Locator)}): each one re-queries the DOM
 *       until the condition holds or the default assertion timeout elapses (5s unless changed
 *       globally via {@code PlaywrightAssertions.setDefaultAssertionTimeout}), then fails with a
 *       diff-style message. This wrapper doesn't expose Playwright's per-call {@code *Options}
 *       overloads, so every call here uses that shared default timeout.</li>
 * </ul>
 */
public final class Hardly {

    private Hardly() {}

    /** Asserts {@code condition} is {@code true}. Checked once, immediately — no retry; see the class docs. */
    public static void isTrue(boolean condition, String context) {
        com.dnestr.base.assertions.Hardly.isTrue(condition, context);
    }

    /** Asserts {@code condition} is {@code false}. Checked once, immediately — no retry; see the class docs. */
    public static void isFalse(boolean condition, String context) {
        com.dnestr.base.assertions.Hardly.isFalse(condition, context);
    }

    /** Asserts {@code actual} equals {@code expected}. Checked once, immediately — no retry; see the class docs. */
    public static <T> void isEqual(T actual, T expected, String context) {
        com.dnestr.base.assertions.Hardly.isEqual(actual, expected, context);
    }

    /** Asserts the locator's text equals {@code expected} exactly (after whitespace normalization), retrying until it matches or the assertion timeout elapses. */
    public static void hasText(Locator locator, String expected) {
        assertThat(locator).hasText(expected);
    }
    /** Asserts the locator's text contains {@code expected} as a substring, retrying until it matches or the assertion timeout elapses. */
    public static void containsText(Locator locator, String expected) {
        assertThat(locator).containsText(expected);
    }
    /** Asserts the locator is visible, retrying until it is or the assertion timeout elapses. */
    public static void isVisible(Locator locator) {
        assertThat(locator).isVisible();
    }
    /** Asserts the locator is hidden or detached, retrying until it is or the assertion timeout elapses. */
    public static void isHidden(Locator locator) {
        assertThat(locator).isHidden();
    }
    /** Asserts the locator is enabled, retrying until it is or the assertion timeout elapses. */
    public static void isEnabled(Locator locator) {
        assertThat(locator).isEnabled();
    }
    /** Asserts the locator is disabled, retrying until it is or the assertion timeout elapses. */
    public static void isDisabled(Locator locator) {
        assertThat(locator).isDisabled();
    }
    /** Asserts the locator (checkbox/radio) is checked, retrying until it is or the assertion timeout elapses. */
    public static void isChecked(Locator locator) {
        assertThat(locator).isChecked();
    }
    /** Asserts the locator has attribute {@code name} equal to {@code value}, retrying until it matches or the assertion timeout elapses. */
    public static void hasAttribute(Locator locator, String name, String value) {
        assertThat(locator).hasAttribute(name, value);
    }
}
