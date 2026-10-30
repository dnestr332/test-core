package com.dnestr.mobile.locators;

import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

import java.util.Arrays;
import java.util.stream.Collectors;

import static com.dnestr.mobile.locators.XPathUtils.literal;

/**
 * Locators by test id, for apps that set one on their elements (React Native's {@code testID}). It lands on
 * {@code resource-id} on Android and on {@code name} on iOS, so each locator matches either and works on both.
 */
public final class TestIdLocators {

    private TestIdLocators() {}

    /** The element with this test id, on either platform. */
    public static By byTestId(String testId) {
        return By.xpath("//*[%s]".formatted(matches(testId)));
    }

    /** Any one of these, for a wait whose outcome can be one of several screens. */
    public static By byAnyTestId(String... testIds) {
        String condition = Arrays.stream(testIds).map(TestIdLocators::matches).collect(Collectors.joining(" or "));
        return By.xpath("//*[%s]".formatted(condition));
    }

    /** The first element whose test id starts with {@code prefix}, e.g. any row of a list keyed by record id. */
    public static By firstByTestIdPrefix(String prefix) {
        return By.xpath("(//*[%s])[1]".formatted(startsWith(prefix)));
    }

    /** Every element whose test id starts with {@code prefix}, e.g. to count a list's rows. */
    public static By allByTestIdPrefix(String prefix) {
        return By.xpath("//*[%s]".formatted(startsWith(prefix)));
    }

    /** The predicate for an element's test id on iOS, as a string: {@code mobile: scroll} takes it in that form. */
    public static String iosTestIdPredicate(String testId) {
        return "name == '%s'".formatted(testId.replace("\\", "\\\\").replace("'", "\\'"));
    }

    /** The element with this test id on iOS, by predicate rather than XPath: no page-source snapshot is taken. */
    public static By iosByTestIdPredicate(String testId) {
        return AppiumBy.iOSNsPredicateString(iosTestIdPredicate(testId));
    }

    /** Android's first scrollable container, scrolled until the element with this test id is in view. */
    public static By androidScrollIntoViewByTestId(String testId) {
        String resourceId = testId.replace("\\", "\\\\").replace("\"", "\\\"");
        return AppiumBy.androidUIAutomator(
                "new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().resourceId(\"%s\"))"
                        .formatted(resourceId)
        );
    }

    private static String matches(String testId) {
        String value = literal(testId);
        return "@resource-id=%1$s or @name=%1$s".formatted(value);
    }

    private static String startsWith(String prefix) {
        String value = literal(prefix);
        return "starts-with(@resource-id,%1$s) or starts-with(@name,%1$s)".formatted(value);
    }
}
