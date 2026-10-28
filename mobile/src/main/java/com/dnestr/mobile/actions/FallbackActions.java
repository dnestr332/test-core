package com.dnestr.mobile.actions;

import com.dnestr.mobile.context.TestContext;
import com.dnestr.mobile.utils.AppInteractionUtils;
import com.dnestr.mobile.utils.MobileElementUtils;
import com.dnestr.base.utils.WaitUtils;
import io.appium.java_client.AppiumDriver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.Point;
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

    /** The driver these actions run on, for the gestures {@code ElementActions} performs on a found element. */
    AppiumDriver driver() {
        return driver;
    }

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
        Point center = MobileElementUtils.centerOf(driver.findElement(locator));

        log.info("Fallback tap at center ({}, {}) for {}", center.getX(), center.getY(), locator);
        AppInteractionUtils.tapByCoordinates(center.getX(), center.getY(), driver);
    }

    /** Clicks the element via a platform-native mobile command instead of Selenium's own click: Appium's {@code mobile: clickGesture} on Android, a coordinate {@code mobile: tap} on iOS. */
    public void clickNativeByPlatform(By locator) {
        WebElement el = driver.findElement(locator);

        if (TestContext.isAndroid()) {
            assert ((RemoteWebElement) el).getId() != null;
            driver.executeScript("mobile: clickGesture", Map.of(
                    "elementId", ((RemoteWebElement) el).getId()
            ));
        } else {
            Point center = MobileElementUtils.centerOf(el);
            driver.executeScript("mobile: tap", Map.of(
                    "x", center.getX(),
                    "y", center.getY()
            ));
        }
    }
}
