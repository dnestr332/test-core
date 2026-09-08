package com.dnestr.web.logs;

import com.dnestr.base.logs.BaseFailureCatcher;
import com.dnestr.base.actions.ElementAction;
import com.microsoft.playwright.Locator;

import java.util.function.Supplier;

public class FailureCatcher extends BaseFailureCatcher<Locator> {

    public FailureCatcher(PrettyPrinter prettyPrinter) {
        super(prettyPrinter);
    }

    public <R> R withFailureCapture(ElementAction action, Locator locator, Supplier<R> supplier) {
        return withFailureCapture(action, locator, null, supplier);
    }

    public void withFailureCapture(ElementAction action, Locator locator, Runnable runnable) {
        withFailureCapture(action, locator, null, runnable);
    }

    public void withFailureCapture(ElementAction action, Runnable runnable) {
        withFailureCapture(action, null, null, runnable);
    }
}
