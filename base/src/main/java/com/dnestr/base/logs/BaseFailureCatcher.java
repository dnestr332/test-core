package com.dnestr.base.logs;

import com.dnestr.base.context.TestFailureContext;
import com.dnestr.base.actions.ElementAction;
import lombok.RequiredArgsConstructor;

import java.util.function.Supplier;

@RequiredArgsConstructor
public abstract class BaseFailureCatcher<L> {

    private final BasePrettyPrinter<L> prettyPrinter;

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

    public void withFailureCapture(ElementAction action, L locator, String details, Runnable runnable) {
        withFailureCapture(action, locator, details, () -> {
            runnable.run();
            return null;
        });
    }
}
