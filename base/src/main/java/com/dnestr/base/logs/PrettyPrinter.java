package com.dnestr.base.logs;

import com.dnestr.base.actions.ElementAction;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;

/**
 * Writes one log line per element action once it ends: the action, how long it took, the locator and the value it
 * used. An action slower than {@value #SLOW_SECONDS} s logs at WARN, a failed one at ERROR with the error's first
 * line. A value typed into a field whose locator mentions a password is masked. {@code T} is a platform's locator
 * type, which a subclass renders with {@link #describe}.
 *
 * @param <T> the platform's locator type
 */
@Slf4j(topic = "ElementActions")
public abstract class PrettyPrinter<T> {

    static final double SLOW_SECONDS = 3;

    /** The locator as it reads in a log line, e.g. {@code role=button[name="Save"i]} or {@code id=submit}. */
    protected abstract String describe(T locator);

    public void done(ElementAction action, T locator, String details, double seconds) {
        String line = format(action, locator, details, seconds);
        if (seconds >= SLOW_SECONDS) {
            log.warn("{}  slow", line);
        } else {
            log.info(line);
        }
    }

    public void failed(ElementAction action, T locator, String details, double seconds, Throwable error) {
        String message = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage().split("\\R", 2)[0];
        log.error("{}  failed: {}", format(action, locator, details, seconds), message);
    }

    String format(ElementAction action, T locator, String details, double seconds) {
        StringBuilder line = new StringBuilder(LogStyles.ACTION).append(' ')
                .append(String.format(Locale.ROOT, "%-10s %6.2fs", action.name().toLowerCase(Locale.ROOT), seconds));
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
}
