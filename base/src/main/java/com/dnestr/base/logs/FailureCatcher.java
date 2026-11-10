package com.dnestr.base.logs;

import com.dnestr.base.actions.ElementAction;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * Runs an element action and writes one log line once it ends: the action, how long it took, the locator and the
 * value it used. An action slower than {@value #SLOW_SECONDS} s logs at WARN; a failed one logs at ERROR with the
 * error's first line and is then rethrown unchanged. A value typed into a field whose locator mentions a password is
 * masked. Platform subclasses fix {@code L} to their locator type and render it with {@link #describe}.
 *
 * @param <L> the platform's locator type (e.g. Playwright's {@code Locator}, or Selenium's {@code By})
 */
@Slf4j(topic = "ElementActions")
public abstract class FailureCatcher<L> {

    static final double SLOW_SECONDS = 3;

    /** The locator as it reads in a log line, e.g. {@code role=button[name="Save"i]} or {@code id=submit}. */
    protected abstract String describe(L locator);

    public <R> R withFailureCapture(ElementAction action, L locator, String details, Supplier<R> supplier) {
        long start = System.nanoTime();
        try {
            R result = supplier.get();
            done(action, locator, details, secondsSince(start));
            return result;
        } catch (Throwable t) {
            failed(action, locator, details, secondsSince(start), t);
            throw t;
        }
    }

    public void withFailureCapture(ElementAction action, L locator, String details, Runnable runnable) {
        withFailureCapture(action, locator, details, () -> {
            runnable.run();
            return null;
        });
    }

    void done(ElementAction action, L locator, String details, double seconds) {
        String line = format(action, locator, details, seconds);
        if (seconds >= SLOW_SECONDS) {
            log.warn("{}  slow", line);
        } else {
            log.info(line);
        }
    }

    void failed(ElementAction action, L locator, String details, double seconds, Throwable error) {
        String message = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage().split("\\R", 2)[0];
        log.error("{}  failed: {}", format(action, locator, details, seconds), message);
    }

    String format(ElementAction action, L locator, String details, double seconds) {
        StringBuilder line = new StringBuilder(LogStyles.ACTION).append(' ')
                .append(String.format(Locale.ROOT, "%-10s %6.2fs", action.name(), seconds));
        String described = locator == null ? null : describe(locator);
        if (described != null) {
            line.append("  ").append(described);
        }
        if (details != null) {
            line.append("  ").append(action == ElementAction.TYPE ? typed(details, described) : details);
        }
        return line.toString();
    }

    private static String typed(String value, String locator) {
        boolean secret = locator != null && locator.toLowerCase(Locale.ROOT).contains("password");
        return '"' + (secret ? "********" : value) + '"';
    }

    private static double secondsSince(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000_000.0;
    }
}
