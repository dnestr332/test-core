package com.dnestr.mobile.waits;

import com.dnestr.mobile.config.WaitConfig;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;

import java.time.Duration;
import java.util.List;
import java.util.function.BooleanSupplier;

public class DriverWait {

    private final AppiumDriver driver;
    private final long longTimeout;
    private final long shortTimeout;
    private final long polling;

    public DriverWait(AppiumDriver driver, WaitConfig config) {
        this.driver = driver;
        this.longTimeout = config.longTimeout();
        this.shortTimeout = config.shortTimeout();
        this.polling = config.polling();
    }

    private FluentWait<AppiumDriver> getWait(long timeout) {
        return new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(timeout))
                .pollingEvery(Duration.ofMillis(polling))
                .ignoring(StaleElementReferenceException.class)
                .ignoring(NoSuchElementException.class);
    }

    //region ELEMENT WAITS
    public WebElement visible(By locator) {
        return getWait(longTimeout)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public WebElement visibleShort(By locator) {
        return getWait(shortTimeout)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public boolean notVisible(By locator) {
        return getWait(shortTimeout)
                .until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    public List<WebElement> presentList(By locator) {
        return getWait(longTimeout)
                .until(ExpectedConditions.presenceOfAllElementsLocatedBy(locator));
    }

    public WebElement clickable(By locator) {
        return getWait(longTimeout)
                .until(ExpectedConditions.elementToBeClickable(locator));
    }

    public WebElement clickableShort(By locator) {
        return getWait(shortTimeout)
                .until(ExpectedConditions.elementToBeClickable(locator));
    }
    //endregion

    //region FUNCTIONAL WAITS
    public void until(BooleanSupplier condition) {
        getWait(longTimeout).until(driver -> condition.getAsBoolean());
    }

    public boolean untilAndReturn(BooleanSupplier condition) {
        return getWait(longTimeout).until(driver -> condition.getAsBoolean());
    }

    public void attributeContains(By locator, String attribute, String value) {
        getWait(longTimeout).until(driver -> {
            String attr = driver.findElement(locator).getAttribute(attribute);
            return attr != null && attr.contains(value);
        });
    }
    //endregion
}
