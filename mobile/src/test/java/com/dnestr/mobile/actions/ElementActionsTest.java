package com.dnestr.mobile.actions;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.openqa.selenium.ElementNotInteractableException;
import com.dnestr.mobile.context.TestContext;
import com.dnestr.mobile.logs.FailureCatcher;
import com.dnestr.mobile.logs.MobilePrettyPrinter;
import com.dnestr.mobile.waits.DriverWait;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ElementActionsTest {

    @Mock
    private DriverWait driverWait;
    @Mock
    private FallbackActions fallbackActions;
    @Mock
    private WebElement element;

    private ElementActions elementActions;
    private final By locator = By.id("target");

    @BeforeEach
    void setUp() {
        System.setProperty("platform", "ANDROID");
        FailureCatcher failureCatcher = new FailureCatcher(new MobilePrettyPrinter());
        elementActions = new ElementActions(driverWait, fallbackActions, failureCatcher);
    }

    @AfterEach
    void cleanup() {
        System.clearProperty("platform");
        TestContext.clearPlatform();
    }

    @Test
    void findDefaultDelegatesToDriverWaitOnly() {
        when(driverWait.visible(locator)).thenReturn(element);

        WebElement result = elementActions.find(locator);

        assertThat(result).isSameAs(element);
        verifyNoInteractions(fallbackActions);
    }

    @Test
    void findHardWaitUsesFallbackPauseAndRelocate() {
        when(fallbackActions.relocate(locator)).thenReturn(element);

        WebElement result = elementActions.find(locator, ActionStrategy.HARD_WAIT);

        assertThat(result).isSameAs(element);
        verify(fallbackActions).pause();
        verify(fallbackActions).relocate(locator);
        verifyNoInteractions(driverWait);
    }

    @Test
    void findListReturnsEmptyWhenNothingPresent() {
        when(driverWait.presentList(locator)).thenReturn(List.of());

        List<WebElement> result = elementActions.findList(locator);

        assertThat(result).isEmpty();
    }

    @Test
    void findListReturnsPresentElements() {
        when(driverWait.presentList(locator)).thenReturn(List.of(element));

        List<WebElement> result = elementActions.findList(locator);

        assertThat(result).containsExactly(element);
    }

    @Test
    void clickDefaultHappyPathDoesNotFallBackToNative() {
        when(driverWait.clickable(locator)).thenReturn(element);

        elementActions.click(locator);

        verify(element).click();
        verify(fallbackActions, never()).clickNativeByPlatform(any());
        verify(fallbackActions, never()).tapByElementCenter(any());
    }

    @Test
    void clickDefaultFallsBackToNativeWhenPrimaryClickThrows() {
        when(driverWait.clickable(locator)).thenReturn(element);
        doThrow(new StaleElementReferenceException("stale")).when(element).click();

        elementActions.click(locator);

        verify(fallbackActions).clickNativeByPlatform(locator);
    }

    @Test
    void clickFallsBackToCoordinateTapWhenNativeClickAlsoThrows() {
        when(driverWait.clickable(locator)).thenReturn(element);
        doThrow(new StaleElementReferenceException("stale")).when(element).click();
        doThrow(new RuntimeException("native tap unsupported")).when(fallbackActions).clickNativeByPlatform(locator);

        elementActions.click(locator);

        verify(fallbackActions).tapByElementCenter(locator);
    }

    @Test
    void fastClickUsesShortClickableWait() {
        when(driverWait.clickableShort(locator)).thenReturn(element);

        elementActions.fastClick(locator);

        verify(element).click();
        verify(driverWait).clickableShort(locator);
        verify(driverWait, never()).clickable(locator);
    }

    @Test
    void isVisibleReturnsTrueWhenDisplayed() {
        when(driverWait.visible(locator)).thenReturn(element);
        when(element.isDisplayed()).thenReturn(true);

        assertThat(elementActions.isVisible(locator)).isTrue();
    }

    @Test
    void isVisibleReturnsFalseWhenElementNotFound() {
        when(driverWait.visible(locator)).thenThrow(new NoSuchElementException("missing"));

        assertThat(elementActions.isVisible(locator)).isFalse();
    }

    @Test
    void isEnabledReturnsFalseWhenElementNotFound() {
        when(driverWait.visibleShort(locator)).thenThrow(new NoSuchElementException("missing"));

        assertThat(elementActions.isEnabled(locator)).isFalse();
    }

    @Test
    void isEnabledDelegatesToValidationUtilsForAndroid() {
        System.setProperty("platform", "ANDROID");
        when(driverWait.visibleShort(locator)).thenReturn(element);
        when(element.getAttribute("enabled")).thenReturn("true");
        when(element.getAttribute("clickable")).thenReturn("true");

        assertThat(elementActions.isEnabled(locator)).isTrue();
    }

    @Test
    void textDefaultReturnsOnFirstSuccessfulRead() {
        System.setProperty("platform", "ANDROID");
        when(driverWait.visible(locator)).thenReturn(element);
        when(element.getText()).thenReturn("  hello  ");

        assertThat(elementActions.text(locator)).isEqualTo("hello");
        verifyNoInteractions(fallbackActions);
    }

    @Test
    void textDefaultRetriesOnStaleElementThenSucceeds() {
        System.setProperty("platform", "ANDROID");
        when(driverWait.visible(locator))
                .thenThrow(new StaleElementReferenceException("stale"))
                .thenReturn(element);
        when(element.getText()).thenReturn("recovered");

        assertThat(elementActions.text(locator)).isEqualTo("recovered");
        verify(fallbackActions, atLeastOnce()).shortPause();
    }

    @Test
    void textDefaultFallsBackToReadTextAfterExhaustingRetries() {
        System.setProperty("platform", "ANDROID");
        when(driverWait.visible(locator)).thenThrow(new StaleElementReferenceException("stale"));
        when(fallbackActions.readText(locator)).thenReturn("fallback-text");

        assertThat(elementActions.text(locator)).isEqualTo("fallback-text");
        verify(fallbackActions).readText(locator);
    }

    @Test
    void typeDelegatesClickClearSendKeysToTheResolvedElement() {
        when(driverWait.clickable(locator)).thenReturn(element);

        elementActions.type(locator, "value");

        verify(element).click();
        verify(element).clear();
        verify(element).sendKeys("value");
    }

    //region CONDITIONAL CLICKS
    @Test
    void clickIfVisibleClicksAPresentElement() {
        when(driverWait.visibleShort(locator)).thenReturn(element);
        when(driverWait.clickable(locator)).thenReturn(element);

        assertThat(elementActions.clickIfVisible(locator)).isTrue();
        verify(element).click();
    }

    @Test
    void clickIfVisibleLeavesAnAbsentElementAlone() {
        when(driverWait.visibleShort(locator)).thenThrow(new TimeoutException("not there"));

        assertThat(elementActions.clickIfVisible(locator)).isFalse();
        verify(driverWait, never()).clickable(any());
    }

    @Test
    void clickIfAppearsWaitsForTheGraceThenClicks() {
        when(driverWait.visible(locator, Duration.ofSeconds(3))).thenReturn(element);
        when(driverWait.clickable(locator)).thenReturn(element);

        assertThat(elementActions.clickIfAppears(locator, Duration.ofSeconds(3))).isTrue();
        verify(element).click();
    }

    @Test
    void clickIfAppearsGivesUpAfterTheGrace() {
        when(driverWait.visible(locator, Duration.ofSeconds(3))).thenThrow(new TimeoutException("not there"));

        assertThat(elementActions.clickIfAppears(locator, Duration.ofSeconds(3))).isFalse();
        verify(driverWait, never()).clickable(any());
    }

    @Test
    void clickUntilVisibleClicksAgainUntilTheNextScreenShows() {
        By next = By.id("next");
        when(driverWait.clickable(locator)).thenReturn(element);
        when(driverWait.visibleShort(next)).thenThrow(new TimeoutException("dropped")).thenReturn(element);

        assertThat(elementActions.clickUntilVisible(locator, next, 3)).isTrue();
        verify(element, times(2)).click();
    }

    @Test
    void clickUntilVisibleStopsAfterItsAttempts() {
        By next = By.id("next");
        when(driverWait.clickable(locator)).thenReturn(element);
        when(driverWait.visibleShort(next)).thenThrow(new TimeoutException("dropped"));

        assertThat(elementActions.clickUntilVisible(locator, next, 3)).isFalse();
        verify(element, times(3)).click();
    }

    @Test
    void clickWhileVisibleClicksUntilTheWatchedElementIsGone() {
        By intro = By.id("intro");
        when(driverWait.visibleShort(intro)).thenReturn(element, element).thenThrow(new TimeoutException("gone"));
        when(driverWait.clickable(locator)).thenReturn(element);

        assertThat(elementActions.clickWhileVisible(intro, locator, 5)).isTrue();
        verify(element, times(2)).click();
    }

    @Test
    void clickWhileVisibleReportsAScreenThatStays() {
        By intro = By.id("intro");
        when(driverWait.visibleShort(intro)).thenReturn(element);
        when(driverWait.clickable(locator)).thenReturn(element);

        assertThat(elementActions.clickWhileVisible(intro, locator, 2)).isFalse();
        verify(element, times(2)).click();
    }

    @Test
    void isVisibleWithTimeoutIsFalseAfterATimeout() {
        when(driverWait.visible(locator, Duration.ofSeconds(2))).thenThrow(new TimeoutException("not there"));

        assertThat(elementActions.isVisible(locator, Duration.ofSeconds(2))).isFalse();
    }
    //endregion

    //region GESTURES & TYPING
    @Test
    void longPressHoldsTheFoundElementOnTheActionsDriver() {
        AppiumDriver driver = mock(AppiumDriver.class);
        when(fallbackActions.driver()).thenReturn(driver);
        when(driverWait.visible(locator)).thenReturn(element);
        when(element.getRect()).thenReturn(new Rectangle(new Point(0, 0), new Dimension(100, 40)));

        elementActions.longPress(locator, Duration.ofSeconds(2));

        verify(driver).perform(any());
    }

    @Test
    void doubleTapTapsTheFoundElementOnTheActionsDriver() {
        AppiumDriver driver = mock(AppiumDriver.class);
        when(fallbackActions.driver()).thenReturn(driver);
        when(driverWait.visible(locator)).thenReturn(element);
        when(element.getRect()).thenReturn(new Rectangle(new Point(0, 0), new Dimension(100, 40)));

        elementActions.doubleTap(locator);

        verify(driver).perform(any());
    }

    @Test
    void appendTypesWithoutClickingOrClearing() {
        when(driverWait.visible(locator)).thenReturn(element);

        elementActions.append(locator, "42");

        verify(element).sendKeys("42");
        verify(element, never()).clear();
        verify(element, never()).click();
    }
    //endregion

    @Test
    void clickFallbackTapsNothingWhenTheElementIsNotOnScreen() {
        when(driverWait.clickable(locator)).thenThrow(new TimeoutException("not visible"));
        doThrow(new ElementNotInteractableException("not on screen")).when(fallbackActions).requireTappable(locator);

        assertThatThrownBy(() -> elementActions.click(locator)).hasStackTraceContaining("not on screen");

        verify(fallbackActions, never()).clickNativeByPlatform(any());
        verify(fallbackActions, never()).tapByElementCenter(any());
    }

    @Test
    void isHiddenIsTrueOnceTheElementGoes() {
        assertThat(elementActions.isHidden(locator)).isTrue();

        verify(driverWait).invisibleShort(locator);
    }

    @Test
    void isHiddenIsFalseWhileTheElementStaysDisplayed() {
        doThrow(new TimeoutException("still shown")).when(driverWait).invisibleShort(locator);

        assertThat(elementActions.isHidden(locator)).isFalse();
    }

    /** A sheet slides up when it opens, and UIKit drops a tap made while it moves. */
    @Test
    void clickOnIosWaitsForTheElementToStopMovingThenTapsIt() {
        System.setProperty("platform", "IOS");
        TestContext.clearPlatform();
        WebElement settled = mock(WebElement.class);
        when(driverWait.clickable(locator)).thenReturn(element);
        when(driverWait.untilStable(locator)).thenReturn(settled);

        elementActions.click(locator);

        verify(settled).click();
        verify(element, never()).click();
    }

    @Test
    void clickOnAndroidTapsWithoutWaitingForStillness() {
        when(driverWait.clickable(locator)).thenReturn(element);

        elementActions.click(locator);

        verify(element).click();
        verify(driverWait, never()).untilStable(any());
    }
}
