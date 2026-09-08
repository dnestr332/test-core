package com.dnestr.base.logs;

import com.dnestr.base.actions.ElementAction;
import lombok.extern.slf4j.Slf4j;

import static com.dnestr.base.logs.LogStyles.*;

@Slf4j
public abstract class BasePrettyPrinter<T> {

    protected abstract String getLocatorType(T locator);
    protected abstract String getLocatorValue(T locator);

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

    public void ok(double sec) {
        log.info("   {} {} {} ({} sec)",
                OK_SHORT,
                GREEN + OK + RESET,
                GREEN + "OK" + RESET,
                sec
        );
    }

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

    protected String extractLocatorFromMessage(String message) {
        return null;
    }
}
