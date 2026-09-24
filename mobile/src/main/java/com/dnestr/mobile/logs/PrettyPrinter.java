package com.dnestr.mobile.logs;

import com.dnestr.base.logs.BasePrettyPrinter;
import org.openqa.selenium.By;

/**
 * Mobile (Selenium/Appium {@link By}) specialization of {@code BasePrettyPrinter}: teaches the
 * shared start/ok/fail log formatting how to render a {@link By} locator and how to spot one inside
 * a Selenium exception message. Relies on Selenium's standard {@code By#toString()} shape,
 * {@code "By.<strategy>: <value>"} (e.g. {@code "By.id: submit-button"}) — more stable than the
 * undocumented Playwright format the web module's equivalent class depends on, since {@code By}'s
 * {@code toString()} format is part of Selenium's public, documented behavior.
 */
public class PrettyPrinter extends BasePrettyPrinter<By> {

    /** The part of {@code locator.toString()} before the first {@code ": "} (e.g. {@code "By.id"}); the whole string if there's no {@code ": "} to split on. */
    @Override
    protected String getLocatorType(By locator) {
        String s = locator.toString();
        int separator = s.indexOf(": ");
        return (separator > -1)
                ? s.substring(0, separator)
                : s;
    }

    /** The part of {@code locator.toString()} after the first {@code ": "} (e.g. {@code "submit-button"}); the whole string again if there's no {@code ": "} to split on (so type and value end up identical in that case). */
    @Override
    protected String getLocatorValue(By locator) {
        String s = locator.toString();
        int separator = s.indexOf(": ");
        return (separator > -1)
                ? s.substring(separator + 2)
                : s;
    }

    /**
     * Pulls a locator reference out of a Selenium failure message for {@code fail()}'s extra
     * diagnostic line, by scanning for the literal substring {@code "By."} (the start of a
     * stringified {@link By}) and returning from there to the end of the message. Returns
     * {@code null} if not present, e.g. for a failure unrelated to a locator.
     */
    @Override
    protected String extractLocatorFromMessage(String message) {
        int idx = message.indexOf("By.");
        return (idx > -1)
                ? message.substring(idx).trim()
                : null;
    }
}
