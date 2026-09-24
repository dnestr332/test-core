package com.dnestr.web.logs;

import com.dnestr.base.logs.BasePrettyPrinter;
import com.microsoft.playwright.Locator;

/**
 * Web (Playwright {@link Locator}) specialization of {@code BasePrettyPrinter}: teaches the shared
 * start/ok/fail log formatting how to render a Playwright locator and how to spot one inside a
 * Playwright exception message.
 * <p>
 * <b>This parsing depends on undocumented Playwright internals</b> — specifically that
 * {@code Locator#toString()} looks like {@code "GetByRoleLocator: role=button, name='Submit'"}
 * (type, then {@code ": "}, then value) — and can silently break on a Playwright version bump
 * rather than fail loudly. {@code PrettyPrinterTest} pins down the current format so a change
 * shows up as a failing test instead of malformed log lines.
 */
public class PrettyPrinter extends BasePrettyPrinter<Locator> {

    /** The part of {@code locator.toString()} before the first {@code ": "} (e.g. {@code "GetByRoleLocator"}); the whole string if there's no {@code ": "} to split on. */
    @Override
    protected String getLocatorType(Locator locator) {
        String s = locator.toString();
        int separator = s.indexOf(": ");
        return (separator > -1)
                ? s.substring(0, separator)
                : s;
    }

    /** The part of {@code locator.toString()} after the first {@code ": "} (e.g. {@code "role=button, name='Submit'"}); the whole string again if there's no {@code ": "} to split on (so type and value end up identical in that case). */
    @Override
    protected String getLocatorValue(Locator locator) {
        String s = locator.toString();
        int separator = s.indexOf(": ");
        return (separator > -1)
                ? s.substring(separator + 2)
                : s;
    }

    /**
     * Pulls the locator call out of a Playwright timeout/error message for {@code fail()}'s extra
     * diagnostic line, by scanning for the literal substrings {@code "locator("} or {@code "getBy"}
     * (in that order of preference) and returning from there to the end of the message. Returns
     * {@code null} if neither is present, e.g. for a failure unrelated to a locator.
     */
    @Override
    protected String extractLocatorFromMessage(String message) {
        int idx = message.indexOf("locator(");
        if (idx > -1) {
            return message.substring(idx).trim();
        }

        idx = message.indexOf("getBy");
        if (idx > -1) {
            return message.substring(idx).trim();
        }

        return null;
    }
}
