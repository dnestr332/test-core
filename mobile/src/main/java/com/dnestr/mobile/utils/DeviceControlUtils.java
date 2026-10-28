package com.dnestr.mobile.utils;

import com.dnestr.mobile.context.TestContext;
import com.dnestr.mobile.actions.AppAction;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.InteractsWithApps;
import io.appium.java_client.LocksDevice;
import io.appium.java_client.Location;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidStartScreenRecordingOptions;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.clipboard.HasClipboard;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.IOSStartScreenRecordingOptions;
import io.appium.java_client.remote.SupportsLocation;
import io.appium.java_client.screenrecording.CanRecordScreen;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.ScreenOrientation;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static com.dnestr.base.logs.LogStyles.*;

/**
 * Control of the device and of the app under test from outside it: app lifecycle, background and deep links,
 * orientation, hardware keys, lock, network, location, clipboard, the notification list, the emulator camera and
 * screen recording. Actions inside the app are in {@link AppInteractionUtils}. A method that works on one platform
 * only has that platform in its name and throws {@link UnsupportedOperationException} on the other.
 */
@Slf4j
public class DeviceControlUtils {

    private static final List<String> UDID_CAPABILITIES = List.of("appium:udid", "udid", "appium:deviceUDID", "deviceUDID");

    private DeviceControlUtils() {}

    //region APP & ORIENTATION
    /** Terminates, activates, or restarts (terminate then activate) the app identified by {@code appId}, if {@code driver} supports app lifecycle control; otherwise does nothing. */
    public static void controlApp(AppiumDriver driver, String appId, AppAction action) {
        if (driver instanceof InteractsWithApps appDriver) {
            log.info("📱 Action: {} | App: {}", action, appId);

            switch (action) {
                case TERMINATE -> appDriver.terminateApp(appId);
                case ACTIVATE -> appDriver.activateApp(appId);
                case RESTART -> {
                    appDriver.terminateApp(appId);
                    appDriver.activateApp(appId);
                }
            }
            log.info("✅ Done: {}", action);
        }
    }

    /** Sends the app under test to the background for {@code duration}, then brings it back. */
    public static void runAppInBackground(Duration duration, AppiumDriver driver) {
        ((InteractsWithApps) driver).runAppInBackground(duration);
    }

    /** Opens {@code url} in the app {@code appId} (Android package, iOS bundle id). */
    public static void openDeepLink(String url, String appId, AppiumDriver driver) {
        String appKey = TestContext.isIos() ? "bundleId" : "package";
        driver.executeScript("mobile: deepLink", Map.of("url", url, appKey, appId));
    }

    /** Rotates the device via the platform-appropriate driver cast. Logs and swallows any failure (e.g. the device doesn't support rotation) rather than throwing. */
    private static void setOrientation(AppiumDriver driver, ScreenOrientation orientation) {
        try {
            if (driver instanceof AndroidDriver) {
                ((AndroidDriver) driver).rotate(orientation);
            } else {
                ((IOSDriver) driver).rotate(orientation);
            }
        } catch (Exception e) {
            log.warn("⚠ Failed to set the orientation: {}", e.getMessage());
        }
    }

    /** Rotates the device to landscape. */
    public static void setToLandscape(AppiumDriver driver) {
        setOrientation(driver, ScreenOrientation.LANDSCAPE);
    }

    /** Rotates the device to portrait. */
    public static void setToPortrait(AppiumDriver driver) {
        setOrientation(driver, ScreenOrientation.PORTRAIT);
    }
    //endregion

    //region KEYS & DEVICE STATE
    /** Presses the Home button: a key event on Android, the hardware button on iOS. */
    public static void pressHome(AppiumDriver driver) {
        if (TestContext.isIos()) {
            driver.executeScript("mobile: pressButton", Map.of("name", "home"));
        } else {
            pressAndroidKey(AndroidKey.HOME, driver);
        }
    }

    /** Presses {@code key} as a key event. */
    public static void pressAndroidKey(AndroidKey key, AppiumDriver driver) {
        PlatformGuards.requireAndroid("key events");
        driver.executeScript("mobile: pressKey", Map.of("keycode", key.getCode()));
    }

    /** Presses Android's Back key. */
    public static void pressAndroidBack(AppiumDriver driver) {
        pressAndroidKey(AndroidKey.BACK, driver);
    }

    /** Locks the screen. */
    public static void lockDevice(AppiumDriver driver) {
        ((LocksDevice) driver).lockDevice();
    }

    /** Unlocks the screen. */
    public static void unlockDevice(AppiumDriver driver) {
        ((LocksDevice) driver).unlockDevice();
    }

    /** Turns Wi-Fi and mobile data on or off. */
    public static void setAndroidConnectivity(boolean wifi, boolean data, AppiumDriver driver) {
        PlatformGuards.requireAndroid("connectivity");
        driver.executeScript("mobile: setConnectivity", Map.of("wifi", wifi, "data", data));
    }

    /** Sets the device's reported GPS position. */
    public static void setDeviceLocation(double latitude, double longitude, AppiumDriver driver) {
        ((SupportsLocation) driver).setLocation(new Location(latitude, longitude));
    }

    /** The clipboard's plain-text content. */
    public static String getClipboardText(AppiumDriver driver) {
        return ((HasClipboard) driver).getClipboardText();
    }

    /**
     * The device's udid from the session capabilities, read under each name Appium may have stored it as. On Android,
     * when the session has none, the only attached device's.
     *
     * @throws IllegalStateException if none is found
     */
    public static String getUdid(AppiumDriver driver) {
        for (String key : UDID_CAPABILITIES) {
            Object udid = driver.getCapabilities().getCapability(key);
            if (udid != null && !String.valueOf(udid).isBlank()) {
                return String.valueOf(udid);
            }
        }
        if (TestContext.isAndroid()) {
            return AdbUtils.getSoleAndroidDeviceUdid()
                    .orElseThrow(() -> new IllegalStateException(
                            "No udid on the session and not exactly one Android device attached")
                    );
        }
        throw new IllegalStateException("No udid on the session");
    }
    //endregion

    //region NOTIFICATIONS
    /** Title and text of every notification in the shade, one trimmed string each. */
    @SuppressWarnings("unchecked")
    public static List<String> getAndroidNotifications(AppiumDriver driver) {
        PlatformGuards.requireAndroid("notifications");
        var result = (Map<String, Object>) driver.executeScript("mobile: getNotifications", Map.of());
        assert result != null;
        var items = (List<Map<String, Object>>) result.getOrDefault("statusBarNotifications", List.of());

        return items.stream()
                .filter(item -> !Boolean.TRUE.equals(item.get("isRemoved")))
                .map(item -> {
                    var notification = (Map<String, Object>) item.getOrDefault("notification", Map.of());
                    return "%s %s".formatted(notification.getOrDefault("title", ""), notification.getOrDefault("text", ""))
                            .trim();
                })
                .toList();
    }

    /** Pulls down the notification shade. */
    public static void openAndroidNotifications(AppiumDriver driver) {
        PlatformGuards.requireAndroid("the notification shade");
        driver.executeScript("mobile: openNotifications", Map.of());
    }

    /**
     * Pulls Notification Center down with a swipe from the top edge's left quarter; on a phone with a notch the right
     * side opens Control Center instead.
     */
    public static void openIosNotificationCenter(AppiumDriver driver) {
        PlatformGuards.requireIos("Notification Center");
        Dimension size = driver.manage().window().getSize();
        int x = size.getWidth() / 4;
        log.info("{} {} Opening Notification Center", INFO_SHORT, INFO);
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");

        Sequence swipe = new Sequence(finger, 0)
                .addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, 2))
                .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(finger.createPointerMove(Duration.ofMillis(400), PointerInput.Origin.viewport(), x, size.getHeight() * 3 / 5))
                .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

        driver.perform(List.of(swipe));
    }

    /** Turns heads-up (banner) notifications on or off on the device with {@code udid}, through adb. */
    public static void setAndroidHeadsUpNotifications(String udid, boolean enabled) {
        AdbUtils.runAndroidShell(udid, "settings", "put", "global", "heads_up_notifications_enabled", enabled ? "1" : "0");
    }
    //endregion

    //region MEDIA
    /**
     * Shows {@code image} to the emulator's camera. The emulator must be started with {@code -camera-back virtualscene}.
     *
     * @throws IllegalStateException if the image cannot be read
     */
    public static void injectAndroidEmulatorCameraImage(Path image, AppiumDriver driver) {
        PlatformGuards.requireAndroid("camera image injection");
        try {
            String payload = Base64.getEncoder().encodeToString(Files.readAllBytes(image));
            driver.executeScript("mobile: injectEmulatorCameraImage", Map.of("payload", payload));
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + image, e);
        }
    }

    /** Starts recording the screen, for at most {@code timeLimit}. */
    public static void startScreenRecording(Duration timeLimit, AppiumDriver driver) {
        CanRecordScreen screen = (CanRecordScreen) driver;
        if (TestContext.isAndroid()) {
            screen.startRecordingScreen(new AndroidStartScreenRecordingOptions().withTimeLimit(timeLimit));
        } else {
            screen.startRecordingScreen(new IOSStartScreenRecordingOptions().withTimeLimit(timeLimit));
        }
    }

    /**
     * Stops the recording {@link #startScreenRecording} started and writes it to {@code file} (an mp4), creating its
     * directory.
     *
     * @throws IllegalStateException if the file cannot be written
     */
    public static Path stopScreenRecording(Path file, AppiumDriver driver) {
        String video = ((CanRecordScreen) driver).stopRecordingScreen();
        try {
            Path directory = file.toAbsolutePath().getParent();
            if (directory != null) Files.createDirectories(directory);
            return Files.write(file, Base64.getMimeDecoder().decode(video));
        } catch (IOException e) {
            throw new IllegalStateException("Could not write the recording to " + file, e);
        }
    }
    //endregion
}
