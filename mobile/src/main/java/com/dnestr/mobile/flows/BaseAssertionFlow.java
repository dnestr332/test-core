package com.dnestr.mobile.flows;

import com.dnestr.mobile.assertions.Hardly;
import com.dnestr.base.assertions.Softly;
import lombok.extern.slf4j.Slf4j;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@Slf4j
public abstract class BaseAssertionFlow {

    public void assertTrue(BooleanSupplier condition, String message) {
        Hardly.isTrue(condition.getAsBoolean(), message);
    }

    public <T> void assertEquals(Supplier<T> actual, T expected, String message) {
        Hardly.isEqual(actual.get(), expected, message);
    }

    public void softAssertDoubleEquals(Supplier<Double> actual, double expected, double offset, String message) {
        Softly.isDoubleEqual(actual.get(), expected, offset, message);
    }

    public void softAssertTrue(BooleanSupplier condition, String message) {
        boolean result = false;
        try {
            result = condition.getAsBoolean();
        } catch (RuntimeException e) {
            logSoftAssertionException(e);
        }
        Softly.isTrue(result, message);
    }

    public <T> void softAssertEquals(Supplier<T> actual, T expected, String message) {
        T actualValue = null;
        try {
            actualValue = actual.get();
        } catch (RuntimeException e) {
            logSoftAssertionException(e);
        }
        Softly.isEqual(actualValue, expected, message);
    }

    private void logSoftAssertionException(RuntimeException e) {
        log.warn("Exception was caught during SOFT assertion: {}", e.getMessage());
    }
}

