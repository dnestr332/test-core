package com.dnestr.base.logs;

import com.dnestr.base.actions.ElementAction;
import lombok.RequiredArgsConstructor;

import java.util.function.Supplier;

/**
 * Runs an element action and logs it once it ends through {@link PrettyPrinter}: its duration on success, or the error
 * on failure, which is then rethrown unchanged. Platform subclasses fix {@code L} to their locator type.
 *
 * @param <L> the platform's locator type (e.g. Playwright's {@code Locator}, or Selenium's {@code By})
 */
@RequiredArgsConstructor
public abstract class FailureCatcher<L> {

    private final PrettyPrinter<L> prettyPrinter;

    public <R> R withFailureCapture(ElementAction action, L locator, String details, Supplier<R> supplier) {
        long start = System.nanoTime();
        try {
            R result = supplier.get();
            prettyPrinter.done(action, locator, details, secondsSince(start));
            return result;
        } catch (Throwable t) {
            prettyPrinter.failed(action, locator, details, secondsSince(start), t);
            throw t;
        }
    }

    public void withFailureCapture(ElementAction action, L locator, String details, Runnable runnable) {
        withFailureCapture(action, locator, details, () -> {
            runnable.run();
            return null;
        });
    }

    private static double secondsSince(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000_000.0;
    }
}
