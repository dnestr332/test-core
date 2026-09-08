package com.dnestr.mobile.logs;

import com.dnestr.base.logs.BaseFailureCatcher;
import com.dnestr.base.actions.ElementAction;
import org.openqa.selenium.By;

import java.util.function.Supplier;

public class FailureCatcher extends BaseFailureCatcher<By> {

    public FailureCatcher(PrettyPrinter prettyPrinter) {
        super(prettyPrinter);
    }

    public <R> R withFailureCapture(ElementAction action, By locator, Supplier<R> supplier) {
        return withFailureCapture(action, locator, null, supplier);
    }

    public void withFailureCapture(ElementAction action, By locator, Runnable runnable) {
        withFailureCapture(action, locator, null, runnable);
    }
}
