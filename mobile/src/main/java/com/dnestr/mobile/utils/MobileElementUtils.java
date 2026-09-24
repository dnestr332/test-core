package com.dnestr.mobile.utils;

import com.dnestr.mobile.context.TestContext;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Element read/write helpers that account for Android/iOS differences in how a value or text is actually exposed on a {@link WebElement}. */
public class MobileElementUtils {

    private MobileElementUtils() {}

    /** Clicks the element, clears any existing value, then types {@code value} — the standard "replace a field's content" sequence. */
    public static void updateValue(WebElement element, String value) {
        element.click();
        element.clear();
        element.sendKeys(value);
    }

    /**
     * Returns an element's displayed text, platform-appropriately: on iOS, {@code getText()} is
     * unreliable for many native controls, so this reads the {@code value} accessibility attribute
     * first, falling back to {@code label}, and finally {@code ""} if neither is set; on Android,
     * {@code element.getText()} is used directly.
     */
    public static String getActualText(WebElement element) {
        if (TestContext.isIos()) {
            String value = element.getAttribute("value");
            if (value != null && !value.isBlank()) {
                return value.trim();
            }

            String label = element.getAttribute("label");
            if (label != null && !label.isBlank()) {
                return label.trim();
            }

            return "";
        } else {
            return element.getText().trim();
        }
    }

    /** {@link #getActualText} applied to every element in {@code target}, in order. */
    public static List<String> getListOfText(List<WebElement> target) {
        return target.stream()
                .map(MobileElementUtils::getActualText)
                .map(String::trim)
                .collect(Collectors.toList());
    }
}
