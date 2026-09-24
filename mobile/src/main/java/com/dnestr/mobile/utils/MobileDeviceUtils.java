package com.dnestr.mobile.utils;

import com.dnestr.mobile.context.TestContext;
import com.dnestr.mobile.actions.AppAction;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.InteractsWithApps;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.ScreenOrientation;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import static com.dnestr.base.logs.LogStyles.*;

/** Device-level gestures and controls (taps, swipes, keyboard, app lifecycle, orientation) that operate below the element level, via raw coordinates or Appium's mobile-command execution. */
@Slf4j
public class MobileDeviceUtils {

    private MobileDeviceUtils() {}

    /** Performs a single tap at the given viewport coordinates via a synthetic touch pointer gesture. */
    public static void tapByCoordinates(int x, int y, AppiumDriver driver) {
        log.info("{} {} Tapping coordinates ({}, {})", INFO_SHORT, INFO, x, y);

        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");

        Sequence tap = new Sequence(finger, 1)
                .addAction(finger.createPointerMove(Duration.ZERO,
                        PointerInput.Origin.viewport(), x, y))
                .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

        driver.perform(Collections.singletonList(tap));
    }

    /** Taps near the top-center of the screen (10% down from the top) — e.g. for dismissing a top banner or reaching a safe-area control. */
    public static void tapTopSafeArea(AppiumDriver driver) {
        Dimension size = driver.manage().window().getSize();

        int x = size.getWidth() / 2;
        int y = (int)(size.getHeight() * 0.1);

        MobileDeviceUtils.tapByCoordinates(x, y, driver);
    }

    /** Taps a point unlikely to hit any interactive element (center-horizontal, upper-quarter vertical) — e.g. to dismiss a keyboard/popover by tapping "outside" it. */
    public static void tapNeutralArea(AppiumDriver driver) {
        int w = driver.manage().window().getSize().getWidth();
        int h = driver.manage().window().getSize().getHeight();

        int x = w / 2;
        int y = h / 4;

        tapByCoordinates(x, y, driver);
    }

    /**
     * Performs a swipe gesture in {@code direction} (e.g. {@code "up"}/{@code "down"}), platform-
     * appropriately: on iOS, passed through to Appium's {@code mobile: swipe} as-is; on Android,
     * passed as the {@code direction} of a {@code mobile: swipeGesture} over a fixed screen region
     * ({@code left=100, top=600, width=800, height=1000}, {@code percent=0.85}).
     */
    public static void swipe(AppiumDriver driver, String direction) {
        log.info("{} {} Swipe {} gesture", INFO_SHORT, INFO, direction);

        if (TestContext.isIos()) {
            driver.executeScript("mobile: swipe", Map.of("direction", "%s".formatted(direction)));
        } else {
            driver.executeScript("mobile: swipeGesture",
                    Map.of(
                            "left", 100,
                            "top", 600,
                            "width", 800,
                            "height", 1000,
                            "direction", direction,
                            "percent", 0.85
                    ));
        }
    }

    /** {@link #swipe} with direction {@code "up"}. */
    public static void swipeUp(AppiumDriver driver) {
        swipe(driver, "up");
    }

    /** {@link #swipe} with direction {@code "down"} — see {@link #swipe}'s docs for the current Android caveat (this is actually an "up" swipe on Android). */
    public static void swipeDown(AppiumDriver driver) {
        swipe(driver, "down");
    }

    /** Hides the on-screen keyboard via the driver's native hide-keyboard command; on failure, falls back to a platform-appropriate gesture (see {@link #hideKeyboardNative}), logging a warning either way it happened. */
    public static void hideKeyboard(AppiumDriver driver) {
        try {
            if (TestContext.isAndroid()) {
                ((AndroidDriver) driver).hideKeyboard();
            } else {
                ((IOSDriver) driver).hideKeyboard();
            }
        } catch (Exception e) {
            log.warn("⚠ Failed to hide keyboard, proceeding to native fallback: {}", e.getMessage());
            hideKeyboardNative(driver);
        }
    }

    /** Fallback for {@link #hideKeyboard} when the native command fails: on iOS, taps near the top of the screen; on Android, sends the hardware back-key (keycode 4). Swallows and logs any further failure rather than propagating it — this is already the fallback path. */
    private static void hideKeyboardNative(AppiumDriver driver) {
        try {
            if (TestContext.isIos()) {
                Dimension size = driver.manage().window().getSize();
                int x = size.getWidth() / 2;
                int y = size.getHeight() / 10;

                tapByCoordinates(x, y, driver);
            } else {
                driver.executeScript("mobile: pressKey", Map.of("keycode", 4));
            }
        } catch (Exception e) {
            log.warn("⚠ Failed to hide keyboard by native: {}", e.getMessage());
        }
    }

    /**
     * Disables Android heads-up (banner) notifications for the connected device via a direct
     * {@code adb shell} call — bypasses the driver entirely, so this only works when {@code adb} is
     * on the {@code PATH} and a device/emulator is already connected. Logs and swallows any failure
     * (e.g. {@code adb} not found, no device attached) rather than throwing.
     */
    public static void disableAndroidPushNotifications() {
        try {
            Runtime.getRuntime().exec(new String[]{
                    "adb", "shell", "settings", "put", "global", "heads_up_notifications_enabled", "0"
            });
        } catch (Exception e) {
            log.warn("⚠ Failed to disable Android push notifications: {}", e.getMessage());
        }
    }

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
}