package com.dnestr.base.context;

/**
 * Thread-local holder for the most recent action failure, written by {@code BaseFailureCatcher}
 * right before it rethrows, and read afterward by anything that wants to know what broke without
 * being part of the throw/catch itself — e.g. a failure hook that attaches a screenshot or extra
 * diagnostics to the currently-failing test. One slot per thread, so safe under parallel scenario
 * execution as long as each scenario runs on its own thread; call {@link #clear()} between
 * scenarios (e.g. in an {@code @AfterEach}/{@code @After}) so a thread reused for the next test
 * doesn't see a stale error from a previous one.
 */
public final class TestFailureContext {

    private static final ThreadLocal<Throwable> LAST_ERROR = new ThreadLocal<>();

    private TestFailureContext() {}

    /** Records {@code throwable} as the current thread's last failure. */
    public static void setError(Throwable throwable) {
        LAST_ERROR.set(throwable);
    }

    /** Returns the current thread's last recorded failure, or {@code null} if none is set. */
    public static Throwable getError() {
        return LAST_ERROR.get();
    }

    /** Clears the current thread's recorded failure. */
    public static void clear() {
        LAST_ERROR.remove();
    }
}
