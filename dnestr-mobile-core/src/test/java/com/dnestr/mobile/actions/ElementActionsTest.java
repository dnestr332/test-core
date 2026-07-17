package com.dnestr.mobile.actions;

import com.dnestr.mobile.context.TestContext;
import com.dnestr.mobile.logs.FailureCatcher;
import com.dnestr.mobile.logs.PrettyPrinter;
import com.dnestr.mobile.waits.DriverWait;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;

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
        FailureCatcher failureCatcher = new FailureCatcher(new PrettyPrinter());
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
}
