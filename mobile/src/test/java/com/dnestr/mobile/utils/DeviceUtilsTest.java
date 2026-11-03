package com.dnestr.mobile.utils;

import org.openqa.selenium.InvalidElementStateException;
import com.dnestr.mobile.locators.SystemLocators;
import com.dnestr.mobile.actions.AppAction;
import com.dnestr.mobile.context.TestContext;
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
import org.mockito.MockedStatic;
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
class DeviceUtilsTest {

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

    //region APP & ORIENTATION
    @Test
    void controlAppTerminateOnlyTerminates() {
        DeviceUtils.controlApp(androidDriver, "com.app", AppAction.TERMINATE);

        verify(androidDriver).terminateApp("com.app");
        verify(androidDriver, never()).activateApp(anyString());
    }

    @Test
    void controlAppActivateOnlyActivates() {
        DeviceUtils.controlApp(androidDriver, "com.app", AppAction.ACTIVATE);

        verify(androidDriver).activateApp("com.app");
        verify(androidDriver, never()).terminateApp(anyString());
    }

    @Test
    void controlAppRestartTerminatesThenActivates() {
        DeviceUtils.controlApp(androidDriver, "com.app", AppAction.RESTART);

        InOrder order = inOrder(androidDriver);
        order.verify(androidDriver).terminateApp("com.app");
        order.verify(androidDriver).activateApp("com.app");
    }

    @Test
    void controlAppIgnoresDriverWithoutAppControl() {
        DeviceUtils.controlApp(driver, "com.app", AppAction.RESTART);

        verifyNoInteractions(driver);
    }

    @Test
    void setToLandscapeRotatesAndroidDriver() {
        DeviceUtils.setToLandscape(androidDriver);

        verify(androidDriver).rotate(ScreenOrientation.LANDSCAPE);
    }

    @Test
    void setToPortraitRotatesIosDriver() {
        DeviceUtils.setToPortrait(iosDriver);

        verify(iosDriver).rotate(ScreenOrientation.PORTRAIT);
    }

    @Test
    void setOrientationSwallowsRotationFailure() {
        doThrow(new WebDriverException("not supported")).when(androidDriver).rotate(any(ScreenOrientation.class));

        assertThatCode(() -> DeviceUtils.setToLandscape(androidDriver)).doesNotThrowAnyException();
    }
    @Test
    void runAppInBackgroundSendsTheAppAwayForTheDuration() {
        DeviceUtils.runAppInBackground(Duration.ofSeconds(5), androidDriver);

        verify(androidDriver).runAppInBackground(Duration.ofSeconds(5));
    }

    @Test
    void openDeepLinkNamesTheAndroidPackage() {
        System.setProperty("platform", "ANDROID");

        DeviceUtils.openDeepLink("myle://documents", "com.app", driver);

        verify(driver).executeScript("mobile: deepLink", Map.of("url", "myle://documents", "package", "com.app"));
    }

    @Test
    void openDeepLinkNamesTheIosBundle() {
        System.setProperty("platform", "IOS");

        DeviceUtils.openDeepLink("myle://documents", "com.app", driver);

        verify(driver).executeScript("mobile: deepLink", Map.of("url", "myle://documents", "bundleId", "com.app"));
    }
    //endregion

    //region KEYS & DEVICE STATE
    @Test
    void pressHomeOnAndroidSendsTheHomeKeyEvent() {
        System.setProperty("platform", "ANDROID");

        DeviceUtils.pressHome(driver);

        verify(driver).executeScript("mobile: pressKey", Map.of("keycode", AndroidKey.HOME.getCode()));
    }

    @Test
    void pressHomeOnIosPressesTheHardwareButton() {
        System.setProperty("platform", "IOS");

        DeviceUtils.pressHome(driver);

        verify(driver).executeScript("mobile: pressButton", Map.of("name", "home"));
    }

    @Test
    void pressAndroidBackSendsKeycodeFour() {
        System.setProperty("platform", "ANDROID");

        DeviceUtils.pressAndroidBack(driver);

        verify(driver).executeScript("mobile: pressKey", Map.of("keycode", 4));
    }

    @Test
    void androidOnlyCommandsRefuseIos() {
        System.setProperty("platform", "IOS");

        assertThatThrownBy(() -> DeviceUtils.pressAndroidKey(AndroidKey.BACK, driver))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> DeviceUtils.setAndroidConnectivity(false, false, driver))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> DeviceUtils.getAndroidNotifications(driver))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> DeviceUtils.openAndroidNotifications(driver))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> DeviceUtils.injectAndroidEmulatorCameraImage(Path.of("any.png"), driver))
                .isInstanceOf(UnsupportedOperationException.class);
        verifyNoInteractions(driver);
    }

    @Test
    void lockAndUnlockUseTheDriversLockCommands() {
        DeviceUtils.lockDevice(iosDriver);
        DeviceUtils.unlockDevice(iosDriver);

        InOrder order = inOrder(iosDriver);
        order.verify(iosDriver).lockDevice();
        order.verify(iosDriver).unlockDevice();
    }

    @Test
    void setAndroidConnectivitySetsWifiAndDataSeparately() {
        System.setProperty("platform", "ANDROID");

        DeviceUtils.setAndroidConnectivity(true, false, driver);

        verify(driver).executeScript("mobile: setConnectivity", Map.of("wifi", true, "data", false));
    }

    @Test
    void setDeviceLocationSetsLatitudeAndLongitude() {
        ArgumentCaptor<Location> location = ArgumentCaptor.forClass(Location.class);

        DeviceUtils.setDeviceLocation(40.6, -73.9, androidDriver);

        verify(androidDriver).setLocation(location.capture());
        assertThat(location.getValue().getLatitude()).isEqualTo(40.6);
        assertThat(location.getValue().getLongitude()).isEqualTo(-73.9);
    }

    @Test
    void getClipboardTextReadsThePlainTextClipboard() {
        when(iosDriver.getClipboardText()).thenReturn("3915 Flatlands Ave");

        assertThat(DeviceUtils.getClipboardText(iosDriver)).isEqualTo("3915 Flatlands Ave");
    }

    @Test
    void getUdidReadsTheFirstNamespacedCapabilityThatIsSet() {
        System.setProperty("platform", "ANDROID");
        when(driver.getCapabilities()).thenReturn(new MutableCapabilities(Map.of("appium:deviceUDID", "emulator-5554")));

        assertThat(DeviceUtils.getUdid(driver)).isEqualTo("emulator-5554");
    }

    @Test
    void getUdidOnIosWithoutTheCapabilityThrows() {
        System.setProperty("platform", "IOS");
        when(driver.getCapabilities()).thenReturn(new MutableCapabilities(Map.of("appium:udid", " ")));

        assertThatThrownBy(() -> DeviceUtils.getUdid(driver))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No udid on the session");
    }
    //endregion

    //region NOTIFICATIONS
    @Test
    void getAndroidNotificationsJoinsTitleAndTextAndSkipsRemovedOnes() {
        System.setProperty("platform", "ANDROID");
        when(driver.executeScript("mobile: getNotifications", Map.of())).thenReturn(Map.of("statusBarNotifications", List.of(
                Map.of("notification", Map.of("title", "Documents", "text", "Please review your documents")),
                Map.of("isRemoved", true, "notification", Map.of("title", "Gone", "text", "dismissed")),
                Map.of("notification", Map.of("text", "No title"))
        )));

        assertThat(DeviceUtils.getAndroidNotifications(driver))
                .containsExactly("Documents Please review your documents", "No title");
    }

    @Test
    void openIosNotificationCenterSwipesDownFromTheTopLeftQuarter() {
        System.setProperty("platform", "IOS");
        stubScreenSize(driver, 400, 800);

        DeviceUtils.openIosNotificationCenter(driver);

        List<Map<String, Object>> actions = performedActions(driver);
        assertThat(actions).extracting(a -> a.get("type")).containsExactly("pointerMove", "pointerDown", "pointerMove", "pointerUp");
        assertThat(actions.get(0)).containsEntry("x", 100).containsEntry("y", 2);
        assertThat(actions.get(2)).containsEntry("x", 100).containsEntry("y", 480);
    }

    @Test
    void openIosNotificationCenterRefusesAndroid() {
        System.setProperty("platform", "ANDROID");

        assertThatThrownBy(() -> DeviceUtils.openIosNotificationCenter(driver))
                .isInstanceOf(UnsupportedOperationException.class);
        verifyNoInteractions(driver);
    }

    @Test
    void openAndroidNotificationsPullsDownTheShade() {
        System.setProperty("platform", "ANDROID");

        DeviceUtils.openAndroidNotifications(driver);

        verify(driver).executeScript("mobile: openNotifications", Map.of());
    }
    //endregion

    //region MEDIA
    @Test
    void injectAndroidEmulatorCameraImageSendsTheImageAsBase64(@TempDir Path dir) throws Exception {
        System.setProperty("platform", "ANDROID");
        Path image = Files.write(dir.resolve("doc.png"), new byte[]{1, 2, 3});

        DeviceUtils.injectAndroidEmulatorCameraImage(image, driver);

        verify(driver).executeScript("mobile: injectEmulatorCameraImage", Map.of("payload", "AQID"));
    }

    @Test
    void injectAndroidEmulatorCameraImageReportsAMissingFile(@TempDir Path dir) {
        System.setProperty("platform", "ANDROID");

        assertThatThrownBy(() -> DeviceUtils.injectAndroidEmulatorCameraImage(dir.resolve("missing.png"), driver))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing.png");
        verifyNoInteractions(driver);
    }

    @Test
    void stopScreenRecordingWritesTheDecodedVideo(@TempDir Path dir) throws Exception {
        when(iosDriver.stopRecordingScreen()).thenReturn(Base64.getMimeEncoder().encodeToString(new byte[]{9, 8, 7}));

        Path saved = DeviceUtils.stopScreenRecording(dir.resolve("videos/run.mp4"), iosDriver);

        assertThat(Files.readAllBytes(saved)).containsExactly(9, 8, 7);
    }

    @Test
    void startScreenRecordingUsesThePlatformsOptions() {
        System.setProperty("platform", "ANDROID");

        DeviceUtils.startScreenRecording(Duration.ofMinutes(5), androidDriver);

        verify(androidDriver).startRecordingScreen(any(AndroidStartScreenRecordingOptions.class));
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

    @Test
    void getForegroundAppIdReadsTheActiveAppsBundleIdOnIos() {
        System.setProperty("platform", "ios");
        when(driver.executeScript("mobile: activeAppInfo")).thenReturn(Map.of("bundleId", "com.apple.mobilephone", "pid", 42));

        assertThat(DeviceUtils.getForegroundAppId(driver)).isEqualTo("com.apple.mobilephone");
    }

    @Test
    void getForegroundAppIdReadsTheResumedActivityOnAndroid() {
        System.setProperty("platform", "android");
        MutableCapabilities capabilities = new MutableCapabilities();
        capabilities.setCapability("appium:udid", "emulator-5554");
        when(driver.getCapabilities()).thenReturn(capabilities);
        try (MockedStatic<AdbUtils> adb = mockStatic(AdbUtils.class)) {
            adb.when(() -> AdbUtils.getAndroidForegroundPackage("emulator-5554")).thenReturn("com.google.android.dialer");

            assertThat(DeviceUtils.getForegroundAppId(driver)).isEqualTo("com.google.android.dialer");
        }
    }

    @Test
    void hideKeyboardOnAndroidUsesNativeCommand() {
        System.setProperty("platform", "ANDROID");
        when(androidDriver.isKeyboardShown()).thenReturn(true);

        DeviceUtils.hideKeyboard(androidDriver);

        verify(androidDriver).hideKeyboard();
        verify(androidDriver, never()).executeScript(anyString(), anyMap());
    }

    @Test
    void hideKeyboardDoesNothingWhenNoKeyboardIsShown() {
        System.setProperty("platform", "ANDROID");

        DeviceUtils.hideKeyboard(androidDriver);

        verify(androidDriver, never()).hideKeyboard();
        verify(androidDriver, never()).executeScript(anyString(), anyMap());
    }

    /** Back would leave the screen whenever the "shown" keyboard was not really up, as on an emulator with a hardware keyboard. */
    @Test
    void hideKeyboardOnAndroidDoesNotPressBackWhenTheHideFails() {
        System.setProperty("platform", "ANDROID");
        when(androidDriver.isKeyboardShown()).thenReturn(true);
        doThrow(new WebDriverException("Soft keyboard not present")).when(androidDriver).hideKeyboard();

        assertThatCode(() -> DeviceUtils.hideKeyboard(androidDriver)).doesNotThrowAnyException();

        verify(androidDriver, never()).executeScript(anyString(), anyMap());
        verify(androidDriver, never()).perform(any());
    }

    @Test
    void hideKeyboardOnIosFallsBackToTapNearTop() {
        System.setProperty("platform", "IOS");
        when(iosDriver.isKeyboardShown()).thenReturn(true);
        doThrow(new WebDriverException("no keyboard")).when(iosDriver).hideKeyboard();
        stubScreenSize(iosDriver, 1000, 2000);

        DeviceUtils.hideKeyboard(iosDriver);

        assertThat(firstMove(iosDriver)).containsEntry("x", 500).containsEntry("y", 200);
    }

    @Test
    void hideKeyboardOnIosPressesTheKeyboardsReturnKeyWhenTheHideFails() {
        System.setProperty("platform", "IOS");
        when(iosDriver.isKeyboardShown()).thenReturn(true, false);
        doThrow(new WebDriverException("Did not know how to dismiss the keyboard")).when(iosDriver).hideKeyboard();
        when(iosDriver.findElements(SystemLocators.iosKeyboardDismissKey())).thenReturn(List.of(element));

        DeviceUtils.hideKeyboard(iosDriver);

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

        DeviceUtils.hideKeyboard(iosDriver);

        verify(element).click();
        assertThat(firstMove(iosDriver)).containsEntry("x", 500).containsEntry("y", 200);
    }

    @Test
    void hideKeyboardOnIosSwallowsFallbackFailure() {
        System.setProperty("platform", "IOS");
        when(iosDriver.isKeyboardShown()).thenReturn(true);
        doThrow(new WebDriverException("no keyboard")).when(iosDriver).hideKeyboard();
        when(iosDriver.manage()).thenThrow(new WebDriverException("boom"));

        assertThatCode(() -> DeviceUtils.hideKeyboard(iosDriver)).doesNotThrowAnyException();
    }

    @Test
    void tapIosKeyboardDoneButtonTapsTheToolbarDone() {
        System.setProperty("platform", "IOS");
        when(driver.findElement(SystemLocators.iosKeyboardDoneButton())).thenReturn(element);

        DeviceUtils.tapIosKeyboardDoneButton(driver);

        verify(element).click();
    }

    @Test
    void tapIosKeyboardDoneButtonRefusesAndroid() {
        System.setProperty("platform", "ANDROID");

        assertThatThrownBy(() -> DeviceUtils.tapIosKeyboardDoneButton(driver))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("only available on iOS");
        verifyNoInteractions(driver);
    }

    @Test
    void typeIntoFocusedElementSendsTheKeysWithoutAnElement() {
        DeviceUtils.typeIntoFocusedElement("123456", driver);

        verify(driver).perform(sequences.capture());
        assertThat(sequences.getValue().iterator().next().encode().toString()).contains("keyDown", "value=1", "value=6");
    }
}
