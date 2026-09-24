package com.dnestr.mobile.actions;

import com.dnestr.mobile.context.TestContext;
import com.dnestr.mobile.utils.MobileDeviceUtils;
import com.dnestr.mobile.utils.MobileElementUtils;
import com.dnestr.base.utils.WaitUtils;
import io.appium.java_client.AppiumDriver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;

import java.util.List;
import java.util.Map;

/**
 * Last-resort element interactions used by {@code ElementActions} when its normal
 * {@code DriverWait}-based path fails or a caller explicitly asked for the slow/robust
 * ({@link ActionStrategy#HARD_WAIT}) path. These bypass Selenium's own click/find semantics in
 * favor of raw driver calls (direct {@code findElement}, Appium mobile-gesture commands, coordinate
 * taps) that work around cases where a standard Selenium click doesn't register on a native app
 * element — at the cost of being less precise (a coordinate-based tap can miss if layout shifted)
 * and not verifying the element was actually interactable first.
 */
@Slf4j
@RequiredArgsConstructor
public class FallbackActions {

    private final AppiumDriver driver;

    /** Finds the element directly via the driver, with no wait — for use after {@link #pause}, once the UI is assumed to have settled. */
    public WebElement relocate(By locator) {
        return driver.findElement(locator);
    }

    /** Finds all matching elements directly via the driver, with no wait. */
    public List<WebElement> relocateList(By locator) {
        return driver.findElements(locator);
    }

    /** Finds the element directly via the driver (no wait) and returns its text via {@link MobileElementUtils#getActualText}. */
    public String readText(By locator) {
        return MobileElementUtils.getActualText(driver.findElement(locator));
    }

    /** Blocks for a fixed 3 seconds to let a UI transition finish before {@link #relocate}/{@link #relocateList} are attempted. */
    public void pause() {
        log.warn("UI settling");
        WaitUtils.sleepSeconds(3);
    }

    /** Blocks for a fixed 1 second — a lighter settle pause than {@link #pause}, e.g. between text-read retries. */
    public void shortPause() {
        log.warn("Short UI settling");
        WaitUtils.sleepSeconds(1);
    }

    /** Taps the geometric center of the element's bounding rect via raw coordinates, bypassing Selenium's click entirely — the last-resort fallback when even a native platform click fails. */
    public void tapByElementCenter(By locator) {
        WebElement el = driver.findElement(locator);

        int centerX = el.getRect().getX() + el.getRect().getWidth() / 2;
        int centerY = el.getRect().getY() + el.getRect().getHeight() / 2;

        log.info("Fallback tap at center ({}, {}) for {}", centerX, centerY, locator);
        MobileDeviceUtils.tapByCoordinates(centerX, centerY, driver);
    }

    /** Clicks the element via a platform-native mobile command instead of Selenium's own click: Appium's {@code mobile: clickGesture} on Android, a coordinate {@code mobile: tap} on iOS. */
    public void clickNativeByPlatform(By locator) {
        WebElement el = driver.findElement(locator);

        if (TestContext.isAndroid()) {
            driver.executeScript("mobile: clickGesture", Map.of(
                    "elementId", ((RemoteWebElement) el).getId()
            ));
        } else {
            Rectangle r = el.getRect();
            driver.executeScript("mobile: tap", Map.of(
                    "x", r.getX() + r.getWidth() / 2,
                    "y", r.getY() + r.getHeight() / 2
            ));
        }
    }
}
