package com.dnestr.web.assertions;

import com.microsoft.playwright.Locator;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class Hardly {

    private Hardly() {}

    public static void isTrue(boolean condition, String context) {
        com.dnestr.core.assertions.Hardly.isTrue(condition, context);
    }

    public static void isFalse(boolean condition, String context) {
        com.dnestr.core.assertions.Hardly.isFalse(condition, context);
    }

    public static <T> void isEqual(T actual, T expected, String context) {
        com.dnestr.core.assertions.Hardly.isEqual(actual, expected, context);
    }

    public static void hasText(Locator locator, String expected) {
        assertThat(locator).hasText(expected);
    }

    public static void containsText(Locator locator, String expected) {
        assertThat(locator).containsText(expected);
    }

    public static void isVisible(Locator locator) {
        assertThat(locator).isVisible();
    }

    public static void isHidden(Locator locator) {
        assertThat(locator).isHidden();
    }

    public static void isEnabled(Locator locator) {
        assertThat(locator).isEnabled();
    }

    public static void isDisabled(Locator locator) {
        assertThat(locator).isDisabled();
    }

    public static void isChecked(Locator locator) {
        assertThat(locator).isChecked();
    }

    public static void hasAttribute(Locator locator, String name, String value) {
        assertThat(locator).hasAttribute(name, value);
    }
}
