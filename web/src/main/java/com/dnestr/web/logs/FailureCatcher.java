package com.dnestr.web.logs;

import com.dnestr.base.logs.BaseFailureCatcher;
import com.dnestr.base.actions.ElementAction;
import com.microsoft.playwright.Locator;

import java.util.function.Supplier;

/**
 * Web (Playwright {@link Locator}) specialization of {@code BaseFailureCatcher}: every action in
 * {@code ElementActions} routes through {@link #withFailureCapture}, which logs the action via
 * {@link PrettyPrinter} before it runs, logs success/timing after, and on failure logs the error,
 * records it to {@code TestFailureContext} (so a later step, e.g. a screenshot hook, can inspect
 * what broke), then rethrows the original exception unchanged.
 * <p>
 * The three overloads below just fill in {@code null} for whichever of {@code locator}/{@code details}
 * doesn't apply to a given action (e.g. {@link #withFailureCapture(ElementAction, Runnable)} for a
 * page-level action with no target element) — the real capture logic lives in the base class.
 */
public class FailureCatcher extends BaseFailureCatcher<Locator> {

    public FailureCatcher(PrettyPrinter prettyPrinter) {
        super(prettyPrinter);
    }

    /** For an action on a locator with no extra descriptive text (e.g. a plain click) that returns a value. */
    public <R> R withFailureCapture(ElementAction action, Locator locator, Supplier<R> supplier) {
        return withFailureCapture(action, locator, null, supplier);
    }

    /** For an action on a locator with no extra descriptive text (e.g. a plain click) that returns nothing. */
    public void withFailureCapture(ElementAction action, Locator locator, Runnable runnable) {
        withFailureCapture(action, locator, null, runnable);
    }

    /** For a page-level action with no target locator and no extra descriptive text (e.g. a scroll-to-top). */
    public void withFailureCapture(ElementAction action, Runnable runnable) {
        withFailureCapture(action, null, null, runnable);
    }
}
