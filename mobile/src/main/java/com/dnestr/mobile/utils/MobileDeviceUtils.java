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

@Slf4j
public class MobileDeviceUtils {

    private MobileDeviceUtils() {}

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

    public static void tapTopSafeArea(AppiumDriver driver) {
        Dimension size = driver.manage().window().getSize();

        int x = size.getWidth() / 2;
        int y = (int)(size.getHeight() * 0.1);

        MobileDeviceUtils.tapByCoordinates(x, y, driver);
    }

    public static void tapNeutralArea(AppiumDriver driver) {
        int w = driver.manage().window().getSize().getWidth();
        int h = driver.manage().window().getSize().getHeight();

        int x = w / 2;
        int y = h / 4;

        tapByCoordinates(x, y, driver);
    }

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
                            "direction", "up",
                            "percent", 0.85
                    ));
        }
    }

    public static void swipeUp(AppiumDriver driver) {
        swipe(driver, "up");
    }

    public static void swipeDown(AppiumDriver driver) {
        swipe(driver, "down");
    }

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

    public static void disableAndroidPushNotifications() {
        try {
            Runtime.getRuntime().exec(new String[]{
                    "adb", "shell", "settings", "put", "global", "heads_up_notifications_enabled", "0"
            });
        } catch (Exception e) {
            log.warn("⚠ Failed to disable Android push notifications: {}", e.getMessage());
        }
    }

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

    public static void setToLandscape(AppiumDriver driver) {
        setOrientation(driver, ScreenOrientation.LANDSCAPE);
    }

    public static void setToPortrait(AppiumDriver driver) {
        setOrientation(driver, ScreenOrientation.PORTRAIT);
    }
}