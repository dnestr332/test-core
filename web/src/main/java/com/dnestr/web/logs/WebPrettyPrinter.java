package com.dnestr.web.logs;

import com.dnestr.base.logs.PrettyPrinter;
import com.microsoft.playwright.Locator;

/**
 * Renders a Playwright {@link Locator} for an action log line: its selector without Playwright's
 * {@code Locator@} and {@code internal:} prefixes, e.g. {@code role=button[name="Save"i]}.
 */
public class WebPrettyPrinter extends PrettyPrinter<Locator> {

    @Override
    protected String describe(Locator locator) {
        String selector = locator.toString();
        if (selector.startsWith("Locator@")) {
            selector = selector.substring("Locator@".length());
        }
        return selector.replace("internal:", "");
    }
}
