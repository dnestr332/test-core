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

/**
 * Selenium {@link FluentWait}-backed polling for mobile elements — this is the module's actual
 * "wait for X" mechanism (Selenium/Appium has nothing built in that auto-retries the way
 * Playwright's locators do); {@code ElementActions} builds its find/click/text behavior on top of
 * this rather than calling the driver directly. Every wait here ignores
 * {@link StaleElementReferenceException} and {@link NoSuchElementException} during polling — an
 * element not existing *yet* isn't a failure, only still not existing once the timeout elapses is.
 */
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
    /** Waits (up to the configured long timeout) for the element to be present and visible, returning it. */
    public WebElement visible(By locator) {
        return getWait(longTimeout)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /** Like {@link #visible}, but with the shorter timeout — for a "probably already there" check rather than a full wait. */
    public WebElement visibleShort(By locator) {
        return getWait(shortTimeout)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /** Waits (up to the configured short timeout) for the element to become invisible or absent, returning whether it did. */
    public boolean notVisible(By locator) {
        return getWait(shortTimeout)
                .until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    /** Waits (up to the configured long timeout) for at least one element matching {@code locator} to be present, returning all of them. */
    public List<WebElement> presentList(By locator) {
        return getWait(longTimeout)
                .until(ExpectedConditions.presenceOfAllElementsLocatedBy(locator));
    }

    /** Waits (up to the configured long timeout) for the element to be visible and enabled, returning it. */
    public WebElement clickable(By locator) {
        return getWait(longTimeout)
                .until(ExpectedConditions.elementToBeClickable(locator));
    }

    /** Like {@link #clickable}, but with the shorter timeout. */
    public WebElement clickableShort(By locator) {
        return getWait(shortTimeout)
                .until(ExpectedConditions.elementToBeClickable(locator));
    }
    //endregion

    //region FUNCTIONAL WAITS
    /** Waits (up to the configured long timeout) for an arbitrary predicate to return {@code true}. */
    public void until(BooleanSupplier condition) {
        getWait(longTimeout).until(driver -> condition.getAsBoolean());
    }

    /** Like {@link #until}, but returns the (always-{@code true}) result instead of nothing, for use as an expression. */
    public boolean untilAndReturn(BooleanSupplier condition) {
        return getWait(longTimeout).until(driver -> condition.getAsBoolean());
    }

    /** Waits (up to the configured long timeout) for the element's {@code attribute} to contain {@code value} as a substring. */
    public void attributeContains(By locator, String attribute, String value) {
        getWait(longTimeout).until(driver -> {
            String attr = driver.findElement(locator).getAttribute(attribute);
            return attr != null && attr.contains(value);
        });
    }
    //endregion
}
