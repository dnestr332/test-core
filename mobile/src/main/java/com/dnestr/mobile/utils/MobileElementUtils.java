package com.dnestr.mobile.utils;

import com.dnestr.mobile.context.TestContext;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MobileElementUtils {

    private MobileElementUtils() {}

    public static void updateValue(WebElement element, String value) {
        element.click();
        element.clear();
        element.sendKeys(value);
    }

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

    public static List<String> getListOfText(List<WebElement> target) {
        return target.stream()
                .map(MobileElementUtils::getActualText)
                .map(String::trim)
                .collect(Collectors.toList());
    }
}
