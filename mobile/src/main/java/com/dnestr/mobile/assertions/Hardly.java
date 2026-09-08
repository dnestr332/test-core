package com.dnestr.mobile.assertions;

public final class Hardly {

    private Hardly() {}

    public static void isTrue(boolean condition, String context) {
        com.dnestr.base.assertions.Hardly.isTrue(condition, context);
    }

    public static void isFalse(boolean condition, String context) {
        com.dnestr.base.assertions.Hardly.isFalse(condition, context);
    }

    public static <T> void isEqual(T actual, T expected, String context) {
        com.dnestr.base.assertions.Hardly.isEqual(actual, expected, context);
    }

    public static void fail(String errorMessage) {
        com.dnestr.base.assertions.Hardly.fail(errorMessage);
    }
}
