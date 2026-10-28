package com.dnestr.mobile.utils;

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
class DeviceControlUtilsTest {

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
        DeviceControlUtils.controlApp(androidDriver, "com.app", AppAction.TERMINATE);

        verify(androidDriver).terminateApp("com.app");
        verify(androidDriver, never()).activateApp(anyString());
    }

    @Test
    void controlAppActivateOnlyActivates() {
        DeviceControlUtils.controlApp(androidDriver, "com.app", AppAction.ACTIVATE);

        verify(androidDriver).activateApp("com.app");
        verify(androidDriver, never()).terminateApp(anyString());
    }

    @Test
    void controlAppRestartTerminatesThenActivates() {
        DeviceControlUtils.controlApp(androidDriver, "com.app", AppAction.RESTART);

        InOrder order = inOrder(androidDriver);
        order.verify(androidDriver).terminateApp("com.app");
        order.verify(androidDriver).activateApp("com.app");
    }

    @Test
    void controlAppIgnoresDriverWithoutAppControl() {
        DeviceControlUtils.controlApp(driver, "com.app", AppAction.RESTART);

        verifyNoInteractions(driver);
    }

    @Test
    void setToLandscapeRotatesAndroidDriver() {
        DeviceControlUtils.setToLandscape(androidDriver);

        verify(androidDriver).rotate(ScreenOrientation.LANDSCAPE);
    }

    @Test
    void setToPortraitRotatesIosDriver() {
        DeviceControlUtils.setToPortrait(iosDriver);

        verify(iosDriver).rotate(ScreenOrientation.PORTRAIT);
    }

    @Test
    void setOrientationSwallowsRotationFailure() {
        doThrow(new WebDriverException("not supported")).when(androidDriver).rotate(any(ScreenOrientation.class));

        assertThatCode(() -> DeviceControlUtils.setToLandscape(androidDriver)).doesNotThrowAnyException();
    }
    @Test
    void runAppInBackgroundSendsTheAppAwayForTheDuration() {
        DeviceControlUtils.runAppInBackground(Duration.ofSeconds(5), androidDriver);

        verify(androidDriver).runAppInBackground(Duration.ofSeconds(5));
    }

    @Test
    void openDeepLinkNamesTheAndroidPackage() {
        System.setProperty("platform", "ANDROID");

        DeviceControlUtils.openDeepLink("myle://documents", "com.app", driver);

        verify(driver).executeScript("mobile: deepLink", Map.of("url", "myle://documents", "package", "com.app"));
    }

    @Test
    void openDeepLinkNamesTheIosBundle() {
        System.setProperty("platform", "IOS");

        DeviceControlUtils.openDeepLink("myle://documents", "com.app", driver);

        verify(driver).executeScript("mobile: deepLink", Map.of("url", "myle://documents", "bundleId", "com.app"));
    }
    //endregion

    //region KEYS & DEVICE STATE
    @Test
    void pressHomeOnAndroidSendsTheHomeKeyEvent() {
        System.setProperty("platform", "ANDROID");

        DeviceControlUtils.pressHome(driver);

        verify(driver).executeScript("mobile: pressKey", Map.of("keycode", AndroidKey.HOME.getCode()));
    }

    @Test
    void pressHomeOnIosPressesTheHardwareButton() {
        System.setProperty("platform", "IOS");

        DeviceControlUtils.pressHome(driver);

        verify(driver).executeScript("mobile: pressButton", Map.of("name", "home"));
    }

    @Test
    void pressAndroidBackSendsKeycodeFour() {
        System.setProperty("platform", "ANDROID");

        DeviceControlUtils.pressAndroidBack(driver);

        verify(driver).executeScript("mobile: pressKey", Map.of("keycode", 4));
    }

    @Test
    void androidOnlyCommandsRefuseIos() {
        System.setProperty("platform", "IOS");

        assertThatThrownBy(() -> DeviceControlUtils.pressAndroidKey(AndroidKey.BACK, driver))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> DeviceControlUtils.setAndroidConnectivity(false, false, driver))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> DeviceControlUtils.getAndroidNotifications(driver))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> DeviceControlUtils.openAndroidNotifications(driver))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> DeviceControlUtils.injectAndroidEmulatorCameraImage(Path.of("any.png"), driver))
                .isInstanceOf(UnsupportedOperationException.class);
        verifyNoInteractions(driver);
    }

    @Test
    void lockAndUnlockUseTheDriversLockCommands() {
        DeviceControlUtils.lockDevice(iosDriver);
        DeviceControlUtils.unlockDevice(iosDriver);

        InOrder order = inOrder(iosDriver);
        order.verify(iosDriver).lockDevice();
        order.verify(iosDriver).unlockDevice();
    }

    @Test
    void setAndroidConnectivitySetsWifiAndDataSeparately() {
        System.setProperty("platform", "ANDROID");

        DeviceControlUtils.setAndroidConnectivity(true, false, driver);

        verify(driver).executeScript("mobile: setConnectivity", Map.of("wifi", true, "data", false));
    }

    @Test
    void setDeviceLocationSetsLatitudeAndLongitude() {
        ArgumentCaptor<Location> location = ArgumentCaptor.forClass(Location.class);

        DeviceControlUtils.setDeviceLocation(40.6, -73.9, androidDriver);

        verify(androidDriver).setLocation(location.capture());
        assertThat(location.getValue().getLatitude()).isEqualTo(40.6);
        assertThat(location.getValue().getLongitude()).isEqualTo(-73.9);
    }

    @Test
    void getClipboardTextReadsThePlainTextClipboard() {
        when(iosDriver.getClipboardText()).thenReturn("3915 Flatlands Ave");

        assertThat(DeviceControlUtils.getClipboardText(iosDriver)).isEqualTo("3915 Flatlands Ave");
    }

    @Test
    void getUdidReadsTheFirstNamespacedCapabilityThatIsSet() {
        System.setProperty("platform", "ANDROID");
        when(driver.getCapabilities()).thenReturn(new MutableCapabilities(Map.of("appium:deviceUDID", "emulator-5554")));

        assertThat(DeviceControlUtils.getUdid(driver)).isEqualTo("emulator-5554");
    }

    @Test
    void getUdidOnIosWithoutTheCapabilityThrows() {
        System.setProperty("platform", "IOS");
        when(driver.getCapabilities()).thenReturn(new MutableCapabilities(Map.of("appium:udid", " ")));

        assertThatThrownBy(() -> DeviceControlUtils.getUdid(driver))
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

        assertThat(DeviceControlUtils.getAndroidNotifications(driver))
                .containsExactly("Documents Please review your documents", "No title");
    }

    @Test
    void openIosNotificationCenterSwipesDownFromTheTopLeftQuarter() {
        System.setProperty("platform", "IOS");
        stubScreenSize(driver, 400, 800);

        DeviceControlUtils.openIosNotificationCenter(driver);

        List<Map<String, Object>> actions = performedActions(driver);
        assertThat(actions).extracting(a -> a.get("type")).containsExactly("pointerMove", "pointerDown", "pointerMove", "pointerUp");
        assertThat(actions.get(0)).containsEntry("x", 100).containsEntry("y", 2);
        assertThat(actions.get(2)).containsEntry("x", 100).containsEntry("y", 480);
    }

    @Test
    void openIosNotificationCenterRefusesAndroid() {
        System.setProperty("platform", "ANDROID");

        assertThatThrownBy(() -> DeviceControlUtils.openIosNotificationCenter(driver))
                .isInstanceOf(UnsupportedOperationException.class);
        verifyNoInteractions(driver);
    }

    @Test
    void openAndroidNotificationsPullsDownTheShade() {
        System.setProperty("platform", "ANDROID");

        DeviceControlUtils.openAndroidNotifications(driver);

        verify(driver).executeScript("mobile: openNotifications", Map.of());
    }
    //endregion

    //region MEDIA
    @Test
    void injectAndroidEmulatorCameraImageSendsTheImageAsBase64(@TempDir Path dir) throws Exception {
        System.setProperty("platform", "ANDROID");
        Path image = Files.write(dir.resolve("doc.png"), new byte[]{1, 2, 3});

        DeviceControlUtils.injectAndroidEmulatorCameraImage(image, driver);

        verify(driver).executeScript("mobile: injectEmulatorCameraImage", Map.of("payload", "AQID"));
    }

    @Test
    void injectAndroidEmulatorCameraImageReportsAMissingFile(@TempDir Path dir) {
        System.setProperty("platform", "ANDROID");

        assertThatThrownBy(() -> DeviceControlUtils.injectAndroidEmulatorCameraImage(dir.resolve("missing.png"), driver))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing.png");
        verifyNoInteractions(driver);
    }

    @Test
    void stopScreenRecordingWritesTheDecodedVideo(@TempDir Path dir) throws Exception {
        when(iosDriver.stopRecordingScreen()).thenReturn(Base64.getMimeEncoder().encodeToString(new byte[]{9, 8, 7}));

        Path saved = DeviceControlUtils.stopScreenRecording(dir.resolve("videos/run.mp4"), iosDriver);

        assertThat(Files.readAllBytes(saved)).containsExactly(9, 8, 7);
    }

    @Test
    void startScreenRecordingUsesThePlatformsOptions() {
        System.setProperty("platform", "ANDROID");

        DeviceControlUtils.startScreenRecording(Duration.ofMinutes(5), androidDriver);

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
}
