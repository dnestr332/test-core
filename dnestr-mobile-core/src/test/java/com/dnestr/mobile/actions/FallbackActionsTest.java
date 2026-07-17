package com.dnestr.mobile.actions;

import com.dnestr.mobile.context.TestContext;
import com.dnestr.mobile.utils.MobileDeviceUtils;
import io.appium.java_client.AppiumDriver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
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

        try (MockedStatic<MobileDeviceUtils> deviceUtils = mockStatic(MobileDeviceUtils.class)) {
            fallbackActions.tapByElementCenter(locator);

            // Rectangle ctor is (x, y, height, width): x=10, y=20, height=50, width=100 -> center (60, 45)
            deviceUtils.verify(() -> MobileDeviceUtils.tapByCoordinates(eq(60), eq(45), eq(driver)));
        }
    }

    @Test
    void clickNativeByPlatformUsesClickGestureOnAndroid() {
        System.setProperty("platform", "ANDROID");
        RemoteWebElement remoteElement = mock(RemoteWebElement.class);
        when(driver.findElement(locator)).thenReturn(remoteElement);
        when(remoteElement.getId()).thenReturn("element-id-123");

        FallbackActions fallbackActions = new FallbackActions(driver);
        fallbackActions.clickNativeByPlatform(locator);

        verify(driver).executeScript("mobile: clickGesture", Map.of("elementId", "element-id-123"));
    }

    @Test
    void clickNativeByPlatformUsesTapOnIos() {
        System.setProperty("platform", "IOS");
        RemoteWebElement remoteElement = mock(RemoteWebElement.class);
        when(driver.findElement(locator)).thenReturn(remoteElement);
        when(remoteElement.getRect()).thenReturn(new Rectangle(0, 0, 20, 40));

        FallbackActions fallbackActions = new FallbackActions(driver);
        fallbackActions.clickNativeByPlatform(locator);

        verify(driver).executeScript("mobile: tap", Map.of("x", 20, "y", 10));
    }
}
