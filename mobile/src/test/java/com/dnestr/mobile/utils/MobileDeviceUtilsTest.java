package com.dnestr.mobile.utils;

import com.dnestr.mobile.actions.AppAction;
import com.dnestr.mobile.context.TestContext;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.ScreenOrientation;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Sequence;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MobileDeviceUtilsTest {

    @Mock
    private AppiumDriver driver;
    @Mock
    private AndroidDriver androidDriver;
    @Mock
    private IOSDriver iosDriver;
    @Mock
    private WebElement element;
    @Captor
    private ArgumentCaptor<Collection<Sequence>> sequences;

    @AfterEach
    void cleanup() {
        System.clearProperty("platform");
        TestContext.clearPlatform();
    }

    //region TAPS
    @Test
    void tapTopSafeAreaTapsTopCenter() {
        stubScreenSize(driver, 1000, 2000);

        MobileDeviceUtils.tapTopSafeArea(driver);

        assertThat(firstMove(driver)).containsEntry("x", 500).containsEntry("y", 200);
    }

    @Test
    void tapNeutralAreaTapsUpperQuarterCenter() {
        stubScreenSize(driver, 1000, 2000);

        MobileDeviceUtils.tapNeutralArea(driver);

        assertThat(firstMove(driver)).containsEntry("x", 500).containsEntry("y", 500);
    }
    //endregion

    //region GESTURES
    @Test
    void longPressHoldsAtElementCenterForGivenDuration() {
        when(element.getRect()).thenReturn(rect(100, 200, 50, 80));

        MobileDeviceUtils.longPress(element, Duration.ofMillis(1500), driver);

        List<Map<String, Object>> actions = performedActions(driver);
        assertThat(actions).extracting(a -> a.get("type"))
                .containsExactly("pointerMove", "pointerDown", "pause", "pointerUp");
        assertThat(actions.get(0)).containsEntry("x", 125).containsEntry("y", 240);
        assertThat(actions.get(2)).containsEntry("duration", 1500L);
    }

    @Test
    void drawWithFingerDrawsZigzagWithoutLiftingFinger() {
        MobileDeviceUtils.drawWithFinger(rect(0, 0, 400, 300), 4, driver);

        List<Map<String, Object>> actions = performedActions(driver);
        assertThat(actions).extracting(a -> a.get("type"))
                .containsExactly("pointerMove", "pointerDown",
                        "pointerMove", "pointerMove", "pointerMove", "pointerMove",
                        "pointerUp");
        // start: left = 400/8, top = 300/3; step = 400*3/4/4 = 75; bottom = 300*2/3
        assertThat(actions).extracting(a -> a.get("x"))
                .containsExactly(50, null, 125, 200, 275, 350, null);
        assertThat(actions).extracting(a -> a.get("y"))
                .containsExactly(100, null, 200, 100, 200, 100, null);
    }

    @Test
    void drawWithFingerOffsetsByBoxOrigin() {
        MobileDeviceUtils.drawWithFinger(rect(10, 20, 400, 300), 1, driver);

        List<Map<String, Object>> actions = performedActions(driver);
        assertThat(actions.get(0)).containsEntry("x", 60).containsEntry("y", 120);
        assertThat(actions.get(2)).containsEntry("x", 360).containsEntry("y", 220);
    }

    @Test
    void drawWithFingerRejectsNonPositiveStrokes() {
        Rectangle box = rect(0, 0, 400, 300);

        assertThatThrownBy(() -> MobileDeviceUtils.drawWithFinger(box, 0, driver))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("strokes");
        verifyNoInteractions(driver);
    }

    @Test
    void swipeOnIosUsesMobileSwipeWithDirectionOnly() {
        System.setProperty("platform", "IOS");

        MobileDeviceUtils.swipe(driver, "up");

        verify(driver).executeScript("mobile: swipe", Map.of("direction", "up"));
    }

    @Test
    void swipeOnAndroidUsesSwipeGestureOverFixedRegion() {
        System.setProperty("platform", "ANDROID");

        MobileDeviceUtils.swipe(driver, "down");

        verify(driver).executeScript("mobile: swipeGesture", Map.of(
                "left", 100,
                "top", 600,
                "width", 800,
                "height", 1000,
                "direction", "down",
                "percent", 0.85
        ));
    }
    //endregion

    //region KEYBOARD
    @Test
    void hideKeyboardOnAndroidUsesNativeCommand() {
        System.setProperty("platform", "ANDROID");

        MobileDeviceUtils.hideKeyboard(androidDriver);

        verify(androidDriver).hideKeyboard();
        verify(androidDriver, never()).executeScript(anyString(), anyMap());
    }

    @Test
    void hideKeyboardOnAndroidFallsBackToBackKey() {
        System.setProperty("platform", "ANDROID");
        doThrow(new WebDriverException("no keyboard")).when(androidDriver).hideKeyboard();

        MobileDeviceUtils.hideKeyboard(androidDriver);

        verify(androidDriver).executeScript("mobile: pressKey", Map.of("keycode", 4));
    }

    @Test
    void hideKeyboardOnIosFallsBackToTapNearTop() {
        System.setProperty("platform", "IOS");
        doThrow(new WebDriverException("no keyboard")).when(iosDriver).hideKeyboard();
        stubScreenSize(iosDriver, 1000, 2000);

        MobileDeviceUtils.hideKeyboard(iosDriver);

        assertThat(firstMove(iosDriver)).containsEntry("x", 500).containsEntry("y", 200);
    }

    @Test
    void hideKeyboardSwallowsFallbackFailure() {
        System.setProperty("platform", "ANDROID");
        doThrow(new WebDriverException("no keyboard")).when(androidDriver).hideKeyboard();
        when(androidDriver.executeScript(anyString(), anyMap())).thenThrow(new WebDriverException("boom"));

        assertThatCode(() -> MobileDeviceUtils.hideKeyboard(androidDriver)).doesNotThrowAnyException();
    }
    //endregion

    //region APP & ORIENTATION
    @Test
    void controlAppTerminateOnlyTerminates() {
        MobileDeviceUtils.controlApp(androidDriver, "com.app", AppAction.TERMINATE);

        verify(androidDriver).terminateApp("com.app");
        verify(androidDriver, never()).activateApp(anyString());
    }

    @Test
    void controlAppActivateOnlyActivates() {
        MobileDeviceUtils.controlApp(androidDriver, "com.app", AppAction.ACTIVATE);

        verify(androidDriver).activateApp("com.app");
        verify(androidDriver, never()).terminateApp(anyString());
    }

    @Test
    void controlAppRestartTerminatesThenActivates() {
        MobileDeviceUtils.controlApp(androidDriver, "com.app", AppAction.RESTART);

        InOrder order = inOrder(androidDriver);
        order.verify(androidDriver).terminateApp("com.app");
        order.verify(androidDriver).activateApp("com.app");
    }

    @Test
    void controlAppIgnoresDriverWithoutAppControl() {
        MobileDeviceUtils.controlApp(driver, "com.app", AppAction.RESTART);

        verifyNoInteractions(driver);
    }

    @Test
    void setToLandscapeRotatesAndroidDriver() {
        MobileDeviceUtils.setToLandscape(androidDriver);

        verify(androidDriver).rotate(ScreenOrientation.LANDSCAPE);
    }

    @Test
    void setToPortraitRotatesIosDriver() {
        MobileDeviceUtils.setToPortrait(iosDriver);

        verify(iosDriver).rotate(ScreenOrientation.PORTRAIT);
    }

    @Test
    void setOrientationSwallowsRotationFailure() {
        doThrow(new WebDriverException("not supported")).when(androidDriver).rotate(any(ScreenOrientation.class));

        assertThatCode(() -> MobileDeviceUtils.setToLandscape(androidDriver)).doesNotThrowAnyException();
    }
    //endregion

    /** Selenium's {@code Rectangle(x, y, height, width)} constructor takes height before width — build from point + dimension to avoid mixing them up. */
    private static Rectangle rect(int x, int y, int width, int height) {
        return new Rectangle(new Point(x, y), new Dimension(width, height));
    }

    private static void stubScreenSize(AppiumDriver target, int width, int height) {
        WebDriver.Options options = mock(WebDriver.Options.class);
        WebDriver.Window window = mock(WebDriver.Window.class);
        when(target.manage()).thenReturn(options);
        when(options.window()).thenReturn(window);
        when(window.getSize()).thenReturn(new Dimension(width, height));
    }

    /** The first pointer-move of the performed gesture — {@code tapByCoordinates} pads its sequence with a leading pause, so the move isn't always at index 0. */
    private Map<String, Object> firstMove(AppiumDriver target) {
        return performedActions(target).stream()
                .filter(a -> "pointerMove".equals(a.get("type")))
                .findFirst()
                .orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> performedActions(AppiumDriver target) {
        verify(target).perform(sequences.capture());
        assertThat(sequences.getValue()).hasSize(1);
        Map<String, Object> encoded = sequences.getValue().iterator().next().encode();
        return (List<Map<String, Object>>) encoded.get("actions");
    }
}
