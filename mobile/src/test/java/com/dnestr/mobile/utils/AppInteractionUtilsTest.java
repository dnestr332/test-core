package com.dnestr.mobile.utils;

import com.dnestr.mobile.actions.AppAction;
import com.dnestr.mobile.context.TestContext;
import com.dnestr.mobile.locators.SystemLocators;
import io.appium.java_client.AppiumBy;
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
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.ScreenOrientation;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Sequence;

import io.appium.java_client.Location;
import io.appium.java_client.android.AndroidStartScreenRecordingOptions;
import io.appium.java_client.android.nativekey.AndroidKey;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.OutputType;
import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;
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
class AppInteractionUtilsTest {

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

        AppInteractionUtils.tapTopSafeArea(driver);

        assertThat(firstMove(driver)).containsEntry("x", 500).containsEntry("y", 200);
    }

    @Test
    void tapNeutralAreaTapsUpperQuarterCenter() {
        stubScreenSize(driver, 1000, 2000);

        AppInteractionUtils.tapNeutralArea(driver);

        assertThat(firstMove(driver)).containsEntry("x", 500).containsEntry("y", 500);
    }
    //endregion

    //region GESTURES
    @Test
    void longPressHoldsAtElementCenterForGivenDuration() {
        when(element.getRect()).thenReturn(rect(100, 200, 50, 80));

        AppInteractionUtils.longPress(element, Duration.ofMillis(1500), driver);

        List<Map<String, Object>> actions = performedActions(driver);
        assertThat(actions).extracting(a -> a.get("type"))
                .containsExactly("pointerMove", "pointerDown", "pause", "pointerUp");
        assertThat(actions.get(0)).containsEntry("x", 125).containsEntry("y", 240);
        assertThat(actions.get(2)).containsEntry("duration", 1500L);
    }

    @Test
    void doubleTapTapsTheCenterTwiceWithAPauseBetween() {
        when(element.getRect()).thenReturn(rect(100, 200, 50, 80));

        AppInteractionUtils.doubleTap(element, driver);

        List<Map<String, Object>> actions = performedActions(driver);
        assertThat(actions).extracting(a -> a.get("type"))
                .containsExactly("pointerMove", "pointerDown", "pointerUp", "pause", "pointerDown", "pointerUp");
        assertThat(actions.get(0)).containsEntry("x", 125).containsEntry("y", 240);
        assertThat(actions.get(3)).containsEntry("duration", 150L);
    }

    @Test
    void dragAndDropHoldsThenMovesToTheTarget() {
        AppInteractionUtils.dragAndDrop(new Point(10, 20), new Point(30, 400), Duration.ofMillis(800), Duration.ofMillis(600), driver);

        List<Map<String, Object>> actions = performedActions(driver);
        assertThat(actions).extracting(a -> a.get("type"))
                .containsExactly("pointerMove", "pointerDown", "pause", "pointerMove", "pointerUp");
        assertThat(actions.get(0)).containsEntry("x", 10).containsEntry("y", 20);
        assertThat(actions.get(2)).containsEntry("duration", 800L);
        assertThat(actions.get(3)).containsEntry("x", 30).containsEntry("y", 400).containsEntry("duration", 600L);
    }

    @Test
    void drawWithFingerDrawsZigzagWithoutLiftingFinger() {
        AppInteractionUtils.drawWithFinger(rect(0, 0, 400, 300), 4, driver);

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
        AppInteractionUtils.drawWithFinger(rect(10, 20, 400, 300), 1, driver);

        List<Map<String, Object>> actions = performedActions(driver);
        assertThat(actions.get(0)).containsEntry("x", 60).containsEntry("y", 120);
        assertThat(actions.get(2)).containsEntry("x", 360).containsEntry("y", 220);
    }

    @Test
    void drawWithFingerRejectsNonPositiveStrokes() {
        Rectangle box = rect(0, 0, 400, 300);

        assertThatThrownBy(() -> AppInteractionUtils.drawWithFinger(box, 0, driver))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("strokes");
        verifyNoInteractions(driver);
    }

    @Test
    void swipeOnIosUsesMobileSwipeWithDirectionOnly() {
        System.setProperty("platform", "IOS");

        AppInteractionUtils.swipe(driver, "up");

        verify(driver).executeScript("mobile: swipe", Map.of("direction", "up"));
    }

    @Test
    void swipeOnAndroidUsesSwipeGestureOverFixedRegion() {
        System.setProperty("platform", "ANDROID");

        AppInteractionUtils.swipe(driver, "down");

        verify(driver).executeScript("mobile: swipeGesture", Map.of(
                "left", 100,
                "top", 600,
                "width", 800,
                "height", 1000,
                "direction", "down",
                "percent", 0.85
        ));
    }

    @Test
    void swipeUpUntilStopsSwipingOnceTheConditionHolds() {
        System.setProperty("platform", "IOS");
        AtomicInteger checks = new AtomicInteger();

        boolean reached = AppInteractionUtils.swipeUpUntil(() -> checks.incrementAndGet() > 2, 5, driver);

        assertThat(reached).isTrue();
        verify(driver, times(2)).executeScript("mobile: swipe", Map.of("direction", "up"));
    }

    @Test
    void swipeUpUntilGivesUpAfterMaxSwipesAndChecksOnceMore() {
        System.setProperty("platform", "IOS");
        AtomicInteger checks = new AtomicInteger();

        boolean reached = AppInteractionUtils.swipeUpUntil(() -> checks.incrementAndGet() > 99, 3, driver);

        assertThat(reached).isFalse();
        assertThat(checks).hasValue(4);
        verify(driver, times(3)).executeScript("mobile: swipe", Map.of("direction", "up"));
    }

    @Test
    void isScrollableBySwipeUpComparesScreenshotsAroundTheSwipe() throws Exception {
        System.setProperty("platform", "IOS");
        when(driver.getScreenshotAs(OutputType.BYTES)).thenReturn(png(Color.WHITE), png(Color.BLACK));

        assertThat(AppInteractionUtils.isScrollableBySwipeUp(driver, 0.01)).isTrue();

        InOrder order = inOrder(driver);
        order.verify(driver).getScreenshotAs(OutputType.BYTES);
        order.verify(driver).executeScript("mobile: swipe", Map.of("direction", "up"));
        order.verify(driver).getScreenshotAs(OutputType.BYTES);
    }

    @Test
    void isScrollableBySwipeUpIsFalseWhenTheScreenDidNotMove() throws Exception {
        System.setProperty("platform", "IOS");
        byte[] same = png(Color.WHITE);
        when(driver.getScreenshotAs(OutputType.BYTES)).thenReturn(same, same);

        assertThat(AppInteractionUtils.isScrollableBySwipeUp(driver, 0.01)).isFalse();
    }

    @Test
    void scrollIntoViewWithTimeoutRetriesUntilTheElementIsReached() {
        System.setProperty("platform", "ANDROID");
        when(driver.findElement(any(By.class)))
                .thenThrow(new NoSuchElementException("still loading"))
                .thenReturn(element);

        assertThat(AppInteractionUtils.scrollIntoView("row", Duration.ofSeconds(5), driver)).isSameAs(element);
        verify(driver, times(2)).findElement(any(By.class));
    }

    @Test
    void scrollIntoViewWithTimeoutRethrowsOnceTimeIsUp() {
        System.setProperty("platform", "ANDROID");
        when(driver.findElement(any(By.class))).thenThrow(new NoSuchElementException("never there"));

        assertThatThrownBy(() -> AppInteractionUtils.scrollIntoView("row", Duration.ZERO, driver))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("never there");
        verify(driver, times(1)).findElement(any(By.class));
    }

    @Test
    void scrollIntoViewOnAndroidSearchesTheScrollableForTheResourceIdAndReturnsTheElement() {
        System.setProperty("platform", "ANDROID");
        By search = AppiumBy.androidUIAutomator("new UiScrollable(new UiSelector().scrollable(true))"
                + ".scrollIntoView(new UiSelector().resourceId(\"doc-row-ssn\"))");
        when(driver.findElement(search)).thenReturn(element);

        assertThat(AppInteractionUtils.scrollIntoView("doc-row-ssn", driver)).isSameAs(element);
        verify(driver, never()).executeScript(anyString(), anyMap());
    }

    @Test
    void scrollIntoViewOnAndroidEscapesQuotesInTheId() {
        System.setProperty("platform", "ANDROID");
        By search = AppiumBy.androidUIAutomator("new UiScrollable(new UiSelector().scrollable(true))"
                + ".scrollIntoView(new UiSelector().resourceId(\"a\\\"b\\\\c\"))");
        when(driver.findElement(search)).thenReturn(element);

        assertThat(AppInteractionUtils.scrollIntoView("a\"b\\c", driver)).isSameAs(element);
    }

    @Test
    void scrollIntoViewOnAndroidThrowsWhenNoScrollReachesTheElement() {
        System.setProperty("platform", "ANDROID");
        when(driver.findElement(any(By.class))).thenThrow(new NoSuchElementException("not found"));

        assertThatThrownBy(() -> AppInteractionUtils.scrollIntoView("missing", driver))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void scrollIntoViewOnIosScrollsByPredicateOnlyThenReturnsTheElement() {
        System.setProperty("platform", "IOS");
        when(driver.findElement(AppiumBy.iOSNsPredicateString("name == 'doc-row-ssn'"))).thenReturn(element);

        assertThat(AppInteractionUtils.scrollIntoView("doc-row-ssn", driver)).isSameAs(element);

        InOrder order = inOrder(driver);
        order.verify(driver).executeScript("mobile: scroll", Map.of("predicateString", "name == 'doc-row-ssn'"));
        order.verify(driver).findElement(AppiumBy.iOSNsPredicateString("name == 'doc-row-ssn'"));
    }

    @Test
    void scrollIntoViewOnIosEscapesQuotesInThePredicate() {
        System.setProperty("platform", "IOS");
        when(driver.findElement(any(By.class))).thenReturn(element);

        AppInteractionUtils.scrollIntoView("it's", driver);

        verify(driver).executeScript("mobile: scroll", Map.of("predicateString", "name == 'it\\'s'"));
    }

    @Test
    void scrollIntoViewOnIosThrowsWhenTheScrollFindsNoElement() {
        System.setProperty("platform", "IOS");
        when(driver.executeScript(eq("mobile: scroll"), anyMap())).thenThrow(new WebDriverException("no match"));

        assertThatThrownBy(() -> AppInteractionUtils.scrollIntoView("missing", driver))
                .isInstanceOf(WebDriverException.class);
        verify(driver, never()).findElement(any(By.class));
    }
    //endregion

    //region KEYBOARD
    @Test
    void hideKeyboardOnAndroidUsesNativeCommand() {
        System.setProperty("platform", "ANDROID");
        when(androidDriver.isKeyboardShown()).thenReturn(true);

        AppInteractionUtils.hideKeyboard(androidDriver);

        verify(androidDriver).hideKeyboard();
        verify(androidDriver, never()).executeScript(anyString(), anyMap());
    }

    @Test
    void hideKeyboardDoesNothingWhenNoKeyboardIsShown() {
        System.setProperty("platform", "ANDROID");

        AppInteractionUtils.hideKeyboard(androidDriver);

        verify(androidDriver, never()).hideKeyboard();
        verify(androidDriver, never()).executeScript(anyString(), anyMap());
    }

    /** Back would leave the screen whenever the "shown" keyboard was not really up, as on an emulator with a hardware keyboard. */
    @Test
    void hideKeyboardOnAndroidDoesNotPressBackWhenTheHideFails() {
        System.setProperty("platform", "ANDROID");
        when(androidDriver.isKeyboardShown()).thenReturn(true);
        doThrow(new WebDriverException("Soft keyboard not present")).when(androidDriver).hideKeyboard();

        assertThatCode(() -> AppInteractionUtils.hideKeyboard(androidDriver)).doesNotThrowAnyException();

        verify(androidDriver, never()).executeScript(anyString(), anyMap());
        verify(androidDriver, never()).perform(any());
    }

    @Test
    void hideKeyboardOnIosFallsBackToTapNearTop() {
        System.setProperty("platform", "IOS");
        when(iosDriver.isKeyboardShown()).thenReturn(true);
        doThrow(new WebDriverException("no keyboard")).when(iosDriver).hideKeyboard();
        stubScreenSize(iosDriver, 1000, 2000);

        AppInteractionUtils.hideKeyboard(iosDriver);

        assertThat(firstMove(iosDriver)).containsEntry("x", 500).containsEntry("y", 200);
    }

    @Test
    void hideKeyboardOnIosPressesTheKeyboardsReturnKeyWhenTheHideFails() {
        System.setProperty("platform", "IOS");
        when(iosDriver.isKeyboardShown()).thenReturn(true, false);
        doThrow(new WebDriverException("Did not know how to dismiss the keyboard")).when(iosDriver).hideKeyboard();
        when(iosDriver.findElements(SystemLocators.iosKeyboardDismissKey())).thenReturn(List.of(element));

        AppInteractionUtils.hideKeyboard(iosDriver);

        verify(element).click();
        verify(iosDriver, never()).perform(any());
    }

    @Test
    void hideKeyboardOnIosTapsNearTopWhenTheReturnKeyLeavesTheKeyboardUp() {
        System.setProperty("platform", "IOS");
        when(iosDriver.isKeyboardShown()).thenReturn(true);
        doThrow(new WebDriverException("Did not know how to dismiss the keyboard")).when(iosDriver).hideKeyboard();
        when(iosDriver.findElements(any(By.class))).thenReturn(List.of(element));
        stubScreenSize(iosDriver, 1000, 2000);

        AppInteractionUtils.hideKeyboard(iosDriver);

        verify(element).click();
        assertThat(firstMove(iosDriver)).containsEntry("x", 500).containsEntry("y", 200);
    }

    @Test
    void hideKeyboardOnIosSwallowsFallbackFailure() {
        System.setProperty("platform", "IOS");
        when(iosDriver.isKeyboardShown()).thenReturn(true);
        doThrow(new WebDriverException("no keyboard")).when(iosDriver).hideKeyboard();
        when(iosDriver.manage()).thenThrow(new WebDriverException("boom"));

        assertThatCode(() -> AppInteractionUtils.hideKeyboard(iosDriver)).doesNotThrowAnyException();
    }

    @Test
    void tapIosKeyboardDoneButtonTapsTheToolbarDone() {
        System.setProperty("platform", "IOS");
        when(driver.findElement(SystemLocators.iosKeyboardDoneButton())).thenReturn(element);

        AppInteractionUtils.tapIosKeyboardDoneButton(driver);

        verify(element).click();
    }

    @Test
    void tapIosKeyboardDoneButtonRefusesAndroid() {
        System.setProperty("platform", "ANDROID");

        assertThatThrownBy(() -> AppInteractionUtils.tapIosKeyboardDoneButton(driver))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("only available on iOS");
        verifyNoInteractions(driver);
    }

    @Test
    void typeIntoFocusedElementSendsTheKeysWithoutAnElement() {
        AppInteractionUtils.typeIntoFocusedElement("123456", driver);

        verify(driver).perform(sequences.capture());
        assertThat(sequences.getValue().iterator().next().encode().toString()).contains("keyDown", "value=1", "value=6");
    }
    //endregion

    //region SCREEN CONTENT
    @Test
    void getPageSourceTextsOnAndroidReadsTextAttributesInOrderWithoutRepeats() {
        System.setProperty("platform", "ANDROID");
        when(driver.getPageSource()).thenReturn("""
                <hierarchy><node text="Call" content-desc="x"><node text=" (212) 555-0100 "/><node text=""/>\
                <node text="Call"/></node></hierarchy>""");

        assertThat(AppInteractionUtils.getPageSourceTexts(driver)).containsExactly("Call", "(212) 555-0100");
    }

    @Test
    void getPageSourceTextsOnIosReadsLabelAndValue() {
        System.setProperty("platform", "IOS");
        when(driver.getPageSource()).thenReturn("""
                <AppiumAUT><XCUIElementTypeApplication label="Phone"><XCUIElementTypeStaticText label="Mobile" value="2125550100"/>\
                </XCUIElementTypeApplication></AppiumAUT>""");

        assertThat(AppInteractionUtils.getPageSourceTexts(driver)).containsExactly("Phone", "Mobile", "2125550100");
    }

    //endregion


    /** Selenium's {@code Rectangle(x, y, height, width)} constructor takes height before width — build from point + dimension to avoid mixing them up. */
    private static Rectangle rect(int x, int y, int width, int height) {
        return new Rectangle(new Point(x, y), new Dimension(width, height));
    }

    private static byte[] png(Color color) throws Exception {
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(color);
        graphics.fillRect(0, 0, 8, 8);
        graphics.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
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
