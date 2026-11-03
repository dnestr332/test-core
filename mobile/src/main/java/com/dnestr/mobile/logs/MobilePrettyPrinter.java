package com.dnestr.mobile.logs;

import com.dnestr.base.logs.PrettyPrinter;
import org.openqa.selenium.By;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders a Selenium or Appium {@link By} for an action log line as {@code strategy=value}, e.g. {@code id=submit} for
 * {@code By.id: submit} or {@code accessibilityId=Menu} for {@code AppiumBy.accessibilityId: Menu}.
 */
public class MobilePrettyPrinter extends PrettyPrinter<By> {

    private static final Pattern BY = Pattern.compile("(?s)^(?:Appium)?By\\.(\\w+): (.*)$");

    @Override
    protected String describe(By locator) {
        Matcher matcher = BY.matcher(locator.toString());
        return matcher.matches() ? matcher.group(1) + "=" + matcher.group(2) : locator.toString();
    }
}
