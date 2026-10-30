package com.dnestr.mobile.actions;

import com.dnestr.mobile.context.TestContext;
import com.dnestr.mobile.utils.AppInteractionUtils;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.remote.AppiumWebElement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FallbackActionsTest {

    @Mock
    private AppiumDriver driver;
    @Mock
    private WebElement element;

    private final By locator = By.id("target");

    @AfterEach
    void cleanup() {
        System.clearProperty("platform");
        TestContext.clearPlatform();
    }

    @Test
    void relocateDelegatesToDriverFindElement() {
        when(driver.findElement(locator)).thenReturn(element);

        FallbackActions fallbackActions = new FallbackActions(driver);

        assertThat(fallbackActions.relocate(locator)).isSameAs(element);
    }

    @Test
    void relocateListDelegatesToDriverFindElements() {
        when(driver.findElements(locator)).thenReturn(List.of(element));

        FallbackActions fallbackActions = new FallbackActions(driver);

        assertThat(fallbackActions.relocateList(locator)).containsExactly(element);
    }

    @Test
    void readTextDelegatesToDriverFoundElementAndroid() {
        System.setProperty("platform", "ANDROID");
        when(driver.findElement(locator)).thenReturn(element);
        when(element.getText()).thenReturn("  value  ");

        FallbackActions fallbackActions = new FallbackActions(driver);

        assertThat(fallbackActions.readText(locator)).isEqualTo("value");
    }

    @Test
    void tapByElementCenterComputesRectCenterAndDelegatesToDeviceUtils() {
        when(driver.findElement(locator)).thenReturn(element);
        when(element.getRect()).thenReturn(new Rectangle(10, 20, 50, 100));

        FallbackActions fallbackActions = new FallbackActions(driver);

        try (MockedStatic<AppInteractionUtils> deviceUtils = mockStatic(AppInteractionUtils.class)) {
            fallbackActions.tapByElementCenter(locator);

            // Rectangle ctor is (x, y, height, width): x=10, y=20, height=50, width=100 -> center (60, 45)
            deviceUtils.verify(() -> AppInteractionUtils.tapByCoordinates(eq(60), eq(45), eq(driver)));
        }
    }

    @Test
    void clickNativeByPlatformUsesClickGestureOnAndroid() {
        System.setProperty("platform", "ANDROID");
        // java-client 11 returns an AppiumWebElement, which is not a RemoteWebElement.
        AppiumWebElement appiumElement = mock(AppiumWebElement.class);
        when(driver.findElement(locator)).thenReturn(appiumElement);
        when(appiumElement.getId()).thenReturn("element-id-123");

        FallbackActions fallbackActions = new FallbackActions(driver);
        fallbackActions.clickNativeByPlatform(locator);

        verify(driver).executeScript("mobile: clickGesture", Map.of("elementId", "element-id-123"));
    }

    @Test
    void clickNativeByPlatformUsesTapOnIos() {
        System.setProperty("platform", "IOS");
        AppiumWebElement appiumElement = mock(AppiumWebElement.class);
        when(driver.findElement(locator)).thenReturn(appiumElement);
        when(appiumElement.getRect()).thenReturn(new Rectangle(0, 0, 20, 40));

        FallbackActions fallbackActions = new FallbackActions(driver);
        fallbackActions.clickNativeByPlatform(locator);

        verify(driver).executeScript("mobile: tap", Map.of("x", 20, "y", 10));
    }

    @Test
    void requireTappableOnIosRefusesAnElementThatIsNotDisplayed() {
        System.setProperty("platform", "IOS");
        when(driver.findElement(locator)).thenReturn(element);
        when(element.isDisplayed()).thenReturn(false);

        assertThatThrownBy(() -> new FallbackActions(driver).requireTappable(locator))
                .isInstanceOf(ElementNotInteractableException.class)
                .hasMessageContaining("is not on screen");
    }

    @ParameterizedTest
    @ValueSource(strings = {"IOS", "ANDROID"})
    void requireTappableRefusesADisabledElementOnBothPlatforms(String platform) {
        System.setProperty("platform", platform);
        when(driver.findElement(locator)).thenReturn(element);
        lenient().when(element.isDisplayed()).thenReturn(true);
        when(element.isEnabled()).thenReturn(false);

        assertThatThrownBy(() -> new FallbackActions(driver).requireTappable(locator))
                .isInstanceOf(ElementNotInteractableException.class)
                .hasMessageContaining("is disabled");
    }

    @ParameterizedTest
    @ValueSource(strings = {"IOS", "ANDROID"})
    void requireTappableAllowsAnEnabledElementOnScreen(String platform) {
        System.setProperty("platform", platform);
        when(driver.findElement(locator)).thenReturn(element);
        lenient().when(element.isDisplayed()).thenReturn(true);
        when(element.isEnabled()).thenReturn(true);

        assertThatCode(() -> new FallbackActions(driver).requireTappable(locator)).doesNotThrowAnyException();
    }

    /** Android's native click is a gesture on the element itself, so an element off screen is not refused there. */
    @Test
    void requireTappableOnAndroidDoesNotCheckWhetherTheElementIsOnScreen() {
        System.setProperty("platform", "ANDROID");
        when(driver.findElement(locator)).thenReturn(element);
        when(element.isEnabled()).thenReturn(true);

        new FallbackActions(driver).requireTappable(locator);

        verify(element, never()).isDisplayed();
    }
}
