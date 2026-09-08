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

@Slf4j
@RequiredArgsConstructor
public class FallbackActions {

    private final AppiumDriver driver;

    public WebElement relocate(By locator) {
        return driver.findElement(locator);
    }

    public List<WebElement> relocateList(By locator) {
        return driver.findElements(locator);
    }

    public String readText(By locator) {
        return MobileElementUtils.getActualText(driver.findElement(locator));
    }

    public void pause() {
        log.warn("UI settling");
        WaitUtils.sleepSeconds(3);
    }

    public void shortPause() {
        log.warn("Short UI settling");
        WaitUtils.sleepSeconds(1);
    }

    public void tapByElementCenter(By locator) {
        WebElement el = driver.findElement(locator);

        int centerX = el.getRect().getX() + el.getRect().getWidth() / 2;
        int centerY = el.getRect().getY() + el.getRect().getHeight() / 2;

        log.info("Fallback tap at center ({}, {}) for {}", centerX, centerY, locator);
        MobileDeviceUtils.tapByCoordinates(centerX, centerY, driver);
    }

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
