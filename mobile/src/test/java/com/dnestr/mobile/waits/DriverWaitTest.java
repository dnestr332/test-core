package com.dnestr.mobile.waits;

import com.dnestr.mobile.config.WaitConfig;
import io.appium.java_client.AppiumDriver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverWaitTest {

    @Mock
    private AppiumDriver driver;
    @Mock
    private WebDriver.TargetLocator targetLocator;
    @Mock
    private Alert alert;
    @Mock
    private WebElement element;

    private DriverWait driverWait;
    private final By locator = By.id("target");

    @BeforeEach
    void setUp() {
        driverWait = new DriverWait(driver, new WaitConfig(1, 0, 10));
    }

    @Test
    void visibleRetriesWhileElementIsMissing() {
        when(driver.findElement(locator))
                .thenThrow(new NoSuchElementException("not yet"))
                .thenReturn(element);
        when(element.isDisplayed()).thenReturn(true);

        WebElement result = driverWait.visible(locator);

        assertThat(result).isSameAs(element);
        verify(driver, times(2)).findElement(locator);
    }

    @Test
    void attributeContainsKeepsPollingWhileAttributeIsNull() {
        when(driver.findElement(locator)).thenReturn(element);
        when(element.getAttribute("class"))
                .thenReturn(null)
                .thenReturn("btn btn-active");

        assertThatCode(() -> driverWait.attributeContains(locator, "class", "active")).doesNotThrowAnyException();
        verify(element, times(2)).getAttribute("class");
    }

    @Test
    void attributeContainsRetriesPastStaleElement() {
        when(driver.findElement(locator)).thenReturn(element);
        when(element.getAttribute("class"))
                .thenThrow(new StaleElementReferenceException("stale"))
                .thenReturn("active");

        assertThatCode(() -> driverWait.attributeContains(locator, "class", "active")).doesNotThrowAnyException();
    }

    @Test
    void attributeContainsTimesOutWhenValueNeverAppears() {
        when(driver.findElement(locator)).thenReturn(element);
        when(element.getAttribute("class")).thenReturn("btn");

        assertThatThrownBy(() -> driverWait.attributeContains(locator, "class", "active"))
                .isInstanceOf(TimeoutException.class);
    }

    @Test
    void waitForAlertReturnsAlertOnceItAppears() {
        when(driver.switchTo()).thenReturn(targetLocator);
        when(targetLocator.alert())
                .thenThrow(new NoAlertPresentException())
                .thenReturn(alert);

        Alert result = driverWait.waitForAlert();

        assertThat(result).isSameAs(alert);
        verify(targetLocator, times(2)).alert();
    }

    @Test
    void waitForAlertTimesOutWhenNoAlertAppears() {
        when(driver.switchTo()).thenReturn(targetLocator);
        when(targetLocator.alert()).thenThrow(new NoAlertPresentException());

        assertThatThrownBy(() -> driverWait.waitForAlert())
                .isInstanceOf(TimeoutException.class);
    }
}
