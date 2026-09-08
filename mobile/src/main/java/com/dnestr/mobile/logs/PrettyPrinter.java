package com.dnestr.mobile.logs;

import com.dnestr.base.logs.BasePrettyPrinter;
import org.openqa.selenium.By;

public class PrettyPrinter extends BasePrettyPrinter<By> {

    @Override
    protected String getLocatorType(By locator) {
        String s = locator.toString();
        int separator = s.indexOf(": ");
        return (separator > -1)
                ? s.substring(0, separator)
                : s;
    }

    @Override
    protected String getLocatorValue(By locator) {
        String s = locator.toString();
        int separator = s.indexOf(": ");
        return (separator > -1)
                ? s.substring(separator + 2)
                : s;
    }

    @Override
    protected String extractLocatorFromMessage(String message) {
        int idx = message.indexOf("By.");
        return (idx > -1)
                ? message.substring(idx).trim()
                : null;
    }
}
