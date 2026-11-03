package com.dnestr.mobile.flows;

import com.dnestr.base.assertions.Softly;
import com.dnestr.base.states.AssertionState;
import com.dnestr.base.states.ButtonState;
import com.dnestr.base.states.FieldState;
import com.dnestr.base.states.VisibleState;
import com.dnestr.base.utils.WaitUtils;
import com.dnestr.mobile.actions.ElementActions;
import com.dnestr.base.assertions.Hardly;
import com.dnestr.mobile.screens.ScreenElement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.core.ConditionTimeoutException;
import org.openqa.selenium.By;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Base for flow classes that assert on screen state — mobile's counterpart to the web module's
 * {@code BaseAssertionFlow}. It holds the element checks every screen shares (visible/hidden, text, enabled,
 * editable), on a screen element or a raw locator, each {@link AssertionState#STRICTLY strictly} (fail now)
 * or {@link AssertionState#SOFTLY softly} (collect), and the generic {@code assert*}/{@code softAssert*} checks they
 * are built on. iOS draws typographic apostrophes, so text is compared with them turned into plain ones.
 * <p>
 * Appium has no auto-retrying assertions, so each check reads once unless it says otherwise. The
 * {@code softAssert*} methods treat a {@link RuntimeException} thrown while reading (e.g. a stale element) as a
 * failed check rather than letting it abort the soft batch; the {@code assert*} methods let it propagate.
 */
@Slf4j
@RequiredArgsConstructor
public abstract class BaseAssertionFlow {

    private static final long TEXT_SETTLE_SECONDS = 10;
    private static final long TEXT_POLL_MILLIS = 500;

    protected final ElementActions elementActions;

    //region ELEMENT CHECKS
    /** Visible waits up to the long timeout for the element; hidden checks it is gone within the short one. */
    public void verifyVisible(ScreenElement element, AssertionState assertion, VisibleState visible) {
        verifyVisible(element.getLocator(), describe(element), assertion, visible);
    }

    /** Asserts the element is visible or hidden; {@code name} labels it in the failure message. */
    public void verifyVisible(By locator, String name, AssertionState assertion, VisibleState visible) {
        BooleanSupplier condition = switch (visible) {
            case VISIBLE -> () -> elementActions.isVisible(locator);
            case HIDDEN -> () -> elementActions.isHidden(locator);
        };
        assertBy(assertion, condition, "❌ %s should be %s".formatted(name, visible.name().toLowerCase()));
    }

    /** Asserts the element's text equals {@code expected}, failing at once or at the end of the scenario per {@code assertion}. */
    public void verifyTextEquals(ScreenElement element, AssertionState assertion, String expected) {
        By locator = element.getLocator();
        String message = "❌ %s text validation".formatted(describe(element));
        switch (assertion) {
            case STRICTLY -> assertEquals(() -> text(locator), expected, message);
            case SOFTLY -> softAssertEquals(() -> text(locator), expected, message);
        }
    }

    /** Softly asserts the element's text contains {@code expected}. */
    public void verifyTextContains(ScreenElement element, String expected) {
        verifyTextContains(element.getLocator(), describe(element), expected);
    }

    /**
     * Soft check that the text matches {@code expected} per {@link #textMatches}, re-read for up to 10 seconds first,
     * since text that follows a server reply can lag the tap that asked for it.
     */
    public void verifyTextContains(By locator, String name, String expected) {
        BooleanSupplier matches = () -> textMatches(text(locator), expected);
        try {
            WaitUtils.waitUntilOnCallerThread(() -> holds(matches), TEXT_SETTLE_SECONDS, TEXT_POLL_MILLIS,
                    "%s to contain <%s>".formatted(name, expected));
        } catch (ConditionTimeoutException stillDifferent) {
            // reported by the assertion below, with the text it last read
        }
        softAssertTrue(matches, "❌ %s should contain <%s>".formatted(name, expected));
    }

    /** Whether {@code actual} satisfies {@code expected} for {@link #verifyTextContains}; override to add keywords. */
    protected boolean textMatches(String actual, String expected) {
        return actual.contains(expected);
    }

    /** Softly asserts the element is enabled or disabled. */
    public void verifyButtonState(ScreenElement element, ButtonState buttonState) {
        By locator = element.getLocator();
        softAssertTrue(
                () -> switch (buttonState) {
                    case ENABLED -> elementActions.isEnabled(locator);
                    case DISABLED -> !elementActions.isEnabled(locator);
                },
                "❌ %s should be %s".formatted(describe(element), buttonState.name().toLowerCase())
        );
    }

    /** Editable is the field's {@code enabled} attribute; a field can be enabled yet not clickable while it has focus. */
    public void verifyFieldState(ScreenElement element, AssertionState assertion, FieldState fieldState) {
        By locator = element.getLocator();
        BooleanSupplier enabled = () -> "true".equalsIgnoreCase(elementActions.find(locator).getAttribute("enabled"));
        BooleanSupplier condition = switch (fieldState) {
            case EDITABLE -> enabled;
            case READ_ONLY -> () -> !enabled.getAsBoolean();
        };
        assertBy(assertion, condition, "❌ %s should be %s".formatted(
                describe(element), fieldState.name().replace("_", " ").toLowerCase()));
    }

    protected void assertBy(AssertionState assertion, BooleanSupplier condition, String message) {
        switch (assertion) {
            case STRICTLY -> assertTrue(condition, message);
            case SOFTLY -> softAssertTrue(condition, message);
        }
    }

    protected String text(By locator) {
        return elementActions.text(locator).replace("’", "'");
    }

    /** {@code MyTripsElement.CARD_PRICE}, which names the screen as well as the element. */
    protected static String describe(ScreenElement element) {
        return element instanceof Enum<?> constant ? constant.getDeclaringClass().getSimpleName() + "." + constant.name() : element.toString();
    }

    private static boolean holds(BooleanSupplier condition) {
        try {
            return condition.getAsBoolean();
        } catch (RuntimeException e) {
            return false;
        }
    }
    //endregion

    //region GENERIC CHECKS
    /** Asserts {@code condition} is {@code true}, failing the test immediately if not. */
    public void assertTrue(BooleanSupplier condition, String message) {
        Hardly.isTrue(condition.getAsBoolean(), message);
    }

    /** Asserts {@code actual.get()} equals {@code expected}, failing the test immediately if not. */
    public <T> void assertEquals(Supplier<T> actual, T expected, String message) {
        Hardly.isEqual(actual.get(), expected, message);
    }

    /** Records a failure (does not throw) if {@code actual.get()} isn't within {@code offset} of {@code expected}. */
    public void softAssertDoubleEquals(Supplier<Double> actual, double expected, double offset, String message) {
        Softly.isDoubleEqual(actual.get(), expected, offset, message);
    }

    /**
     * Records a failure (does not throw) if {@code condition} isn't {@code true}. If evaluating
     * {@code condition} itself throws a {@link RuntimeException}, that's logged and treated as a
     * failed ({@code false}) result rather than propagating.
     */
    public void softAssertTrue(BooleanSupplier condition, String message) {
        boolean result = false;
        try {
            result = condition.getAsBoolean();
        } catch (RuntimeException e) {
            logSoftAssertionException(e);
        }
        Softly.isTrue(result, message);
    }

    /**
     * Records a failure (does not throw) if {@code actual.get()} doesn't equal {@code expected}. If
     * evaluating {@code actual} itself throws a {@link RuntimeException}, that's logged and treated
     * as a {@code null} actual value rather than propagating.
     */
    public <T> void softAssertEquals(Supplier<T> actual, T expected, String message) {
        T actualValue = null;
        try {
            actualValue = actual.get();
        } catch (RuntimeException e) {
            logSoftAssertionException(e);
        }
        Softly.isEqual(actualValue, expected, message);
    }
    //endregion

    private void logSoftAssertionException(RuntimeException e) {
        log.warn("Exception was caught during SOFT assertion: {}", e.getMessage());
    }
}
