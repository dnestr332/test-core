package com.dnestr.mobile.logs;

import com.dnestr.base.logs.BaseFailureCatcher;
import com.dnestr.base.actions.ElementAction;
import org.openqa.selenium.By;

import java.util.function.Supplier;

/**
 * Mobile (Selenium/Appium {@link By}) specialization of {@code BaseFailureCatcher}: every action in
 * {@code ElementActions} routes through {@link #withFailureCapture}, which logs the action via
 * {@link PrettyPrinter} before it runs, logs success/timing after, and on failure logs the error,
 * records it to {@code TestFailureContext}, then rethrows the original exception unchanged.
 * <p>
 * The two overloads below just fill in {@code null} for the {@code details} that doesn't apply to
 * a given action — the real capture logic lives in the base class.
 */
public class FailureCatcher extends BaseFailureCatcher<By> {

    public FailureCatcher(PrettyPrinter prettyPrinter) {
        super(prettyPrinter);
    }

    /** For an action on a locator with no extra descriptive text that returns a value. */
    public <R> R withFailureCapture(ElementAction action, By locator, Supplier<R> supplier) {
        return withFailureCapture(action, locator, null, supplier);
    }

    /** For an action on a locator with no extra descriptive text that returns nothing. */
    public void withFailureCapture(ElementAction action, By locator, Runnable runnable) {
        withFailureCapture(action, locator, null, runnable);
    }
}
