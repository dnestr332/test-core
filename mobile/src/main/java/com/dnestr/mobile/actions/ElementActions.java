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

@Slf4j
@RequiredArgsConstructor
public class ElementActions {

    private final DriverWait driverWait;
    private final FallbackActions fallbackActions;
    private final FailureCatcher failureCatcher;

    //region FIND ELEMENT
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

    public WebElement find(By locator) {
        return find(locator, DEFAULT);
    }
    //endregion

    //region FIND LIST
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

    public List<WebElement> findList(By locator) {
        return findList(locator, DEFAULT);
    }
    //endregion

    //region CLICK/TAP
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

    public void click(By locator) {
        click(locator, DEFAULT);
    }

    public void fastClick(By locator) {
        click(locator, FAST_TRY);
    }

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
    public void type(By locator, String text) {
        failureCatcher.withFailureCapture(
                TYPE, locator, "'" + text + "'",
                () -> MobileElementUtils.updateValue(driverWait.clickable(locator), text)
        );
    }

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

    public String text(By locator) {
        return text(locator, DEFAULT);
    }
    //endregion

    //region CHECKERS
    public boolean isVisible(By locator) {
        try {
            return driverWait.visible(locator).isDisplayed();
        } catch (NoSuchElementException | TimeoutException | StaleElementReferenceException ignored) {
            return false;
        }
    }

    public boolean isQuickVisible(By locator) {
        try {
            WebElement el = driverWait.visibleShort(locator);
            return el != null;
        } catch (Exception ignored) {
            return false;
        }
    }

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
