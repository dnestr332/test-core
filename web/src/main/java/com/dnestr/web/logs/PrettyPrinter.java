package com.dnestr.web.logs;

import com.dnestr.base.logs.BasePrettyPrinter;
import com.microsoft.playwright.Locator;

public class PrettyPrinter extends BasePrettyPrinter<Locator> {

    @Override
    protected String getLocatorType(Locator locator) {
        String s = locator.toString();
        int separator = s.indexOf(": ");
        return (separator > -1)
                ? s.substring(0, separator)
                : s;
    }

    @Override
    protected String getLocatorValue(Locator locator) {
        String s = locator.toString();
        int separator = s.indexOf(": ");
        return (separator > -1)
                ? s.substring(separator + 2)
                : s;
    }

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
