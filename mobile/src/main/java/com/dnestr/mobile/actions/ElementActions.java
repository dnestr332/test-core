package com.dnestr.mobile.actions;

import com.dnestr.base.actions.ElementAction;
import com.dnestr.mobile.logs.FailureCatcher;
import com.dnestr.mobile.utils.MobileValidationUtils;
import com.dnestr.mobile.utils.MobileElementUtils;
import com.dnestr.mobile.waits.DriverWait;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;

import java.util.List;

import static com.dnestr.base.actions.ElementAction.*;
import static com.dnestr.mobile.actions.ActionStrategy.*;

/**
 * Appium/Selenium-backed element actions used across mobile page objects — this module's
 * counterpart to the web module's {@code ElementActions}, but considerably more defensive: native
 * mobile apps are more prone to elements that exist but don't respond to a standard click/read, so
 * most actions here have an explicit fallback path ({@link FallbackActions}) rather than only the
 * single {@code FailureCatcher}-wrapped happy path the web module has. Every action still routes
 * through {@link FailureCatcher} for logging/failure capture; used by composition, passed into
 * page objects, not extended.
 */
@Slf4j
@RequiredArgsConstructor
public class ElementActions {

    private final DriverWait driverWait;
    private final FallbackActions fallbackActions;
    private final FailureCatcher failureCatcher;

    //region FIND ELEMENT
    /**
     * Finds the element per {@code strategy}: {@code DEFAULT} waits for visibility via
     * {@link DriverWait#visible}; {@code HARD_WAIT} pauses for the UI to settle
     * ({@link FallbackActions#pause}) then relocates the element directly with no further wait.
     * {@code FAST_TRY} and {@code NO_WAIT} aren't handled here (only by {@link #click}/{@link #text}) —
     * passing either falls through to the "not found" branch below.
     *
     * @throws NoSuchElementException if the strategy isn't handled, or (via {@link DriverWait}) the
     *                                 element never became visible within its timeout
     */
    public WebElement find(By locator, ActionStrategy strategy) {
        return failureCatcher.withFailureCapture(FIND, locator,
                () -> {
                    switch (strategy) {
                        case HARD_WAIT -> {
                            logFallback(FIND, HARD_WAIT, locator);
                            fallbackActions.pause();
                            return fallbackActions.relocate(locator);
                        }
                        case DEFAULT -> {
                            return driverWait.visible(locator);
                        }
                    }
                    throw new NoSuchElementException("Element is not found using locator: " + locator);
                }
        );
    }

    /** {@link #find(By, ActionStrategy)} with {@link ActionStrategy#DEFAULT}. */
    public WebElement find(By locator) {
        return find(locator, DEFAULT);
    }
    //endregion

    //region FIND LIST
    /**
     * Finds every matching element per {@code strategy}: {@code HARD_WAIT} pauses then relocates
     * directly with no further wait; {@code DEFAULT} waits for at least one element to be present
     * via {@link DriverWait#presentList} and returns the list if non-empty. Any other outcome
     * (including an empty {@code DEFAULT} result, or an unhandled strategy) returns an empty list
     * rather than throwing — unlike {@link #find}, "no elements matched" is a valid, non-exceptional
     * result for a list lookup.
     */
    public List<WebElement> findList(By locator, ActionStrategy strategy) {
        return failureCatcher.withFailureCapture(FIND_LIST, locator,
                () -> {
                    switch (strategy) {
                        case HARD_WAIT -> {
                            logFallback(FIND_LIST, HARD_WAIT, locator);
                            fallbackActions.pause();
                            return fallbackActions.relocateList(locator);
                        }
                        case DEFAULT -> {
                            List<WebElement> list = driverWait.presentList(locator);
                            if (!list.isEmpty()) {
                                return list;
                            }
                        }
                    }
                    return List.of();
                }
        );
    }

    /** {@link #findList(By, ActionStrategy)} with {@link ActionStrategy#DEFAULT}. */
    public List<WebElement> findList(By locator) {
        return findList(locator, DEFAULT);
    }
    //endregion

    //region CLICK/TAP
    /**
     * Clicks the element per {@code strategy}, each with a fallback to {@link #clickNative} if the
     * primary attempt fails: {@code HARD_WAIT} pauses then goes straight to a native click;
     * {@code FAST_TRY} waits with the short timeout for clickability, falling back to native on any
     * exception; {@code DEFAULT} does the same with the long timeout. Any other strategy is silently
     * a no-op (no branch matches, nothing is clicked, no exception is thrown).
     */
    public void click(By locator, ActionStrategy strategy) {
        failureCatcher.withFailureCapture(CLICK, locator, () -> {
                    switch (strategy) {
                        case HARD_WAIT -> {
                            logFallback(CLICK_BY_NATIVE, HARD_WAIT, locator);
                            fallbackActions.pause();
                            clickNative(locator);
                        }
                        case FAST_TRY -> {
                            try {
                                driverWait.clickableShort(locator).click();
                            } catch (Exception e) {
                                logFallback(CLICK_BY_NATIVE, NO_WAIT, locator);
                                clickNative(locator);
                            }
                        }
                        case DEFAULT -> {
                            try {
                                driverWait.clickable(locator).click();
                            } catch (Exception e) {
                                logFallback(CLICK, NO_WAIT, locator);
                                clickNative(locator);
                            }
                        }
                    }
                }
        );
    }

    /** {@link #click(By, ActionStrategy)} with {@link ActionStrategy#DEFAULT}. */
    public void click(By locator) {
        click(locator, DEFAULT);
    }

    /** {@link #click(By, ActionStrategy)} with {@link ActionStrategy#FAST_TRY} — for a call site that expects the element to already be present, so a long wait would only slow down an already-failing test. */
    public void fastClick(By locator) {
        click(locator, FAST_TRY);
    }

    /**
     * Clicks via {@link FallbackActions#clickNativeByPlatform} (a platform-native mobile command),
     * falling back further to {@link FallbackActions#tapByElementCenter} (a raw coordinate tap) if
     * even that fails — the most robust, least precise click path, used when Selenium's own click
     * doesn't register on a native element.
     */
    public void clickNative(By locator) {
        failureCatcher.withFailureCapture(CLICK_BY_NATIVE, locator, () -> {
            try {
                fallbackActions.clickNativeByPlatform(locator);
            } catch (Exception e) {
                log.warn("Native click failed. Trying coordinate tap fallback.");
                fallbackActions.tapByElementCenter(locator);
            }
        });
    }
    //endregion

    //region TYPE & GET TEXT
    /** Waits for the element to be clickable, then replaces its content with {@code text} via {@link MobileElementUtils#updateValue}. */
    public void type(By locator, String text) {
        failureCatcher.withFailureCapture(
                TYPE, locator, "'" + text + "'",
                () -> MobileElementUtils.updateValue(driverWait.clickable(locator), text)
        );
    }

    /**
     * Reads the element's text per {@code strategy}: {@code HARD_WAIT} pauses then reads directly
     * via {@link FallbackActions#readText}; {@code DEFAULT} waits for visibility and reads via
     * {@link MobileElementUtils#getActualText}, retrying up to 3 times if the element goes stale
     * between the wait and the read (a real race on some native apps that re-render the element
     * right after it becomes visible), falling back to {@link FallbackActions#readText} if all
     * retries are exhausted. Any other strategy returns {@code ""} rather than throwing.
     */
    public String text(By locator, ActionStrategy strategy) {
        return failureCatcher.withFailureCapture(GET_TEXT, locator, () -> {
                    switch (strategy) {
                        case HARD_WAIT -> {
                            logFallback(GET_TEXT, HARD_WAIT, locator);
                            fallbackActions.pause();
                            return fallbackActions.readText(locator);
                        }
                        case DEFAULT -> {
                            for (int i = 0; i < 3; i++) {
                                try {
                                    return MobileElementUtils.getActualText(driverWait.visible(locator));
                                } catch (StaleElementReferenceException e) {
                                    log.debug("Text stale (attempt {}), retrying: {}", i + 1, locator);
                                    fallbackActions.shortPause();
                                }
                            }
                            log.warn("⚠ TEXT fallback used after retries for {}", locator);
                            return fallbackActions.readText(locator);
                        }
                    }
                    return "";
                }
        );
    }

    /** {@link #text(By, ActionStrategy)} with {@link ActionStrategy#DEFAULT}. */
    public String text(By locator) {
        return text(locator, DEFAULT);
    }
    //endregion

    //region CHECKERS
    /**
     * Whether the element is present and displayed, waiting up to the long timeout for it to
     * become visible first. Unlike {@link #find}, never throws — not found/timed out/gone stale
     * before the check all count as "not visible" ({@code false}), since this is meant as a
     * boolean check, not a lookup.
     */
    public boolean isVisible(By locator) {
        try {
            return driverWait.visible(locator).isDisplayed();
        } catch (NoSuchElementException | TimeoutException | StaleElementReferenceException ignored) {
            return false;
        }
    }

    /** Like {@link #isVisible}, but waits only up to the short timeout — for a "probably already there" check rather than a full wait. */
    public boolean isQuickVisible(By locator) {
        try {
            WebElement el = driverWait.visibleShort(locator);
            return el != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Whether the element is enabled/interactable, waiting up to the short timeout for it to become
     * visible first, via {@link MobileValidationUtils#isButtonEnabled}. Never throws — not
     * found/timed out/gone stale all count as "not enabled" ({@code false}).
     */
    public boolean isEnabled(By locator) {
        try {
            WebElement el = driverWait.visibleShort(locator);
            return MobileValidationUtils.isButtonEnabled(el);
        } catch (NoSuchElementException | TimeoutException | StaleElementReferenceException ignored) {
            return false;
        }
    }
    //endregion

    private void logFallback(ElementAction action, ActionStrategy strategy, By locator) {
        log.warn("⚠ {} fallback used [{}] for {}", action, strategy, locator);
    }
}
