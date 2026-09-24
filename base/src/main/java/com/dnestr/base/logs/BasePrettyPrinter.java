package com.dnestr.base.logs;

import com.dnestr.base.actions.ElementAction;
import lombok.extern.slf4j.Slf4j;

import static com.dnestr.base.logs.LogStyles.*;

/**
 * Formats the three-part step log line ({@code start}/{@code ok}/{@code fail}) shared by every
 * {@code BaseFailureCatcher}, using ANSI colors and emoji from {@link LogStyles}. {@code T} is a
 * platform's locator type; subclasses (web's and mobile's own {@code PrettyPrinter}) implement
 * {@link #getLocatorType}/{@link #getLocatorValue} to render that platform's locator, and may
 * override {@link #extractLocatorFromMessage} to pull a locator reference back out of a failure
 * message for the extra diagnostic line in {@link #fail}.
 *
 * @param <T> the platform's locator type
 */
@Slf4j
public abstract class BasePrettyPrinter<T> {

    /** Short label for {@code locator}'s kind (e.g. a Playwright locator's strategy name), shown in the {@code [TYPE]} tag of a log line. */
    protected abstract String getLocatorType(T locator);

    /** The rest of {@code locator}'s description (e.g. its selector/matcher value), shown after the {@code [TYPE]} tag of a log line. */
    protected abstract String getLocatorValue(T locator);

    /** Logs the start of {@code action}, with {@code details} appended to the action label and {@code locator}'s type/value rendered if either is non-null. */
    public void start(ElementAction action, T locator, String details) {
        String prefix = BLUE + CLICK + RESET + " ";
        String shortTag = INFO_SHORT;

        String actionLabel = (details == null)
                ? action.name()
                : action.name() + " " + details;

        if (locator == null) {
            log.info("{} {} {}{}{}",
                    prefix,
                    shortTag,
                    GREEN,
                    actionLabel,
                    RESET
            );
            return;
        }

        log.info("{} {} {}{}{}  [{}] {}",
                prefix,
                shortTag,
                GREEN,
                actionLabel,
                RESET,
                getLocatorType(locator),
                getLocatorValue(locator)
        );
    }

    /** Logs a step's success, with its duration in seconds. */
    public void ok(double sec) {
        log.info("   {} {} {} ({} sec)",
                OK_SHORT,
                GREEN + OK + RESET,
                GREEN + "OK" + RESET,
                sec
        );
    }

    /**
     * Logs a step's failure: the first line of {@code t}'s message (or {@code "<no message>"} if
     * none), then a locator reference pulled from that line via {@link #extractLocatorFromMessage}
     * if one is found, then the top 3 frames of {@code t}'s stack trace.
     */
    public void fail(Throwable t) {
        String line = (t.getMessage() == null)
                ? "<no message>"
                : t.getMessage().split("\\R", 2)[0];

        log.error("   {} {} {}",
                FAIL_SHORT,
                RED + FAIL + RESET,
                line
        );

        String locatorLine = extractLocatorFromMessage(line);
        if (locatorLine != null) {
            log.error("     {}{}{}", RED, locatorLine, RESET);
        }

        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < Math.min(3, st.length); i++) {
            log.error("     {}{}{}", RED, st[i], RESET);
        }
    }

    /** Hook for pulling a locator reference out of a failure message, for the extra diagnostic line in {@link #fail}. Default: none found. */
    protected String extractLocatorFromMessage(String message) {
        return null;
    }
}
