package com.dnestr.base.logs;

import com.dnestr.base.context.TestFailureContext;
import com.dnestr.base.actions.ElementAction;
import lombok.RequiredArgsConstructor;

import java.util.function.Supplier;

/**
 * Wraps an action with start/success/failure logging via {@link BasePrettyPrinter}, and on failure
 * records the throwable to {@link TestFailureContext} before rethrowing it unchanged — so a caller
 * gets normal exception propagation while still leaving a trail for diagnostics (e.g. a screenshot
 * hook that reads {@code TestFailureContext.getError()}). Platform-specific subclasses (web's and
 * mobile's own {@code FailureCatcher}) fix {@code L} to their locator type and typically add
 * convenience overloads that default {@code details}/{@code locator} to {@code null}.
 *
 * @param <L> the platform's locator type (e.g. Playwright's {@code Locator}, or an Appium equivalent)
 */
@RequiredArgsConstructor
public abstract class BaseFailureCatcher<L> {

    private final BasePrettyPrinter<L> prettyPrinter;

    /**
     * Logs {@code action} starting, runs {@code supplier}, then logs and returns its result. On any
     * {@link Throwable}, logs the failure, records it to {@link TestFailureContext}, and rethrows
     * it as-is (the original exception, not wrapped).
     */
    public <R> R withFailureCapture(ElementAction action, L locator, String details, Supplier<R> supplier) {
        long start = System.currentTimeMillis();
        prettyPrinter.start(action, locator, details);

        try {
            R result = supplier.get();
            double sec = (System.currentTimeMillis() - start) / 1000.0;
            prettyPrinter.ok(sec);
            return result;
        } catch (Throwable t) {
            prettyPrinter.fail(t);
            TestFailureContext.setError(t);
            throw t;
        }
    }

    /** {@code Runnable} overload of {@link #withFailureCapture(ElementAction, Object, String, Supplier)} for actions with no return value. */
    public void withFailureCapture(ElementAction action, L locator, String details, Runnable runnable) {
        withFailureCapture(action, locator, details, () -> {
            runnable.run();
            return null;
        });
    }
}
