package com.dnestr.base.context;

public final class TestFailureContext {

    private static final ThreadLocal<Throwable> LAST_ERROR = new ThreadLocal<>();

    private TestFailureContext() {}

    public static void setError(Throwable throwable) {
        LAST_ERROR.set(throwable);
    }

    public static Throwable getError() {
        return LAST_ERROR.get();
    }

    public static void clear() {
        LAST_ERROR.remove();
    }
}
