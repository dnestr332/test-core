package com.dnestr.mobile.actions;

import com.dnestr.mobile.context.TestContext;
import com.dnestr.mobile.locators.SystemLocators;
import com.dnestr.mobile.utils.DeviceControlUtils;
import com.dnestr.mobile.waits.DriverWait;
import io.appium.java_client.AppiumDriver;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Taps on the OS's own UI ({@link SystemLocators}): alerts, permission prompts, notifications and the camera. Used by
 * composition like {@link ElementActions}, with the same driver.
 */
@RequiredArgsConstructor
public class SystemActions {

    /** iOS permission alerts label their buttons differently per permission and version; the first one present is tapped. */
    private static final List<String> IOS_ALLOW_LABELS = List.of("Allow While Using App", "Allow", "OK");
    private static final List<String> IOS_DENY_LABELS = List.of("Don’t Allow", "Don't Allow");

    private final ElementActions elementActions;
    private final DriverWait driverWait;
    private final AppiumDriver driver;

    /** Waits for the native alert and taps the button labelled {@code label} (ignoring case on Android). */
    public void tapAlertButton(String label) {
        if (TestContext.isAndroid()) {
            elementActions.click(SystemLocators.androidAlertButton(label));
            return;
        }
        driverWait.waitForAlert();
        driver.executeScript("mobile: alert", Map.of("action", "accept", "buttonLabel", label));
    }

    /**
     * Answers the OS permission prompt. Allowing picks "While using the app" where it is offered, so the app does
     * not get more access than a user would normally grant.
     *
     * @throws IllegalStateException on iOS, if the alert has none of the expected button labels
     */
    @SuppressWarnings("unchecked")
    public void answerPermissionPrompt(boolean allow) {
        if (TestContext.isAndroid()) {
            elementActions.click(allow
                    ? SystemLocators.androidPermissionAllowButton()
                    : SystemLocators.androidPermissionDenyButton());
            return;
        }
        driverWait.waitForAlert();
        List<String> buttons = (List<String>) driver.executeScript("mobile: alert", Map.of("action", "getButtons"));
        assert buttons != null;
        String label = (allow ? IOS_ALLOW_LABELS : IOS_DENY_LABELS).stream()
                .filter(buttons::contains)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No %s button among %s".formatted(allow ? "allow" : "deny", buttons)));
        driver.executeScript("mobile: alert", Map.of("action", "accept", "buttonLabel", label));
    }

    /**
     * Opens the notification list — Android's shade, iOS's Notification Center — and taps the notification whose text
     * contains {@code text}.
     */
    public void tapNotification(String text) {
        if (TestContext.isAndroid()) {
            DeviceControlUtils.openAndroidNotifications(driver);
            elementActions.click(SystemLocators.androidNotification(text));
            return;
        }
        DeviceControlUtils.openIosNotificationCenter(driver);
        elementActions.click(SystemLocators.iosNotification(text));
    }

    /**
     * Takes a photo with the emulator's camera app and confirms it, pressing the shutter up to {@code shutterAttempts}
     * times, since a press can land before the camera is ready. It photographs whatever
     * {@link DeviceControlUtils#injectAndroidEmulatorCameraImage} put in front of it.
     */
    public void takeAndroidEmulatorCameraPhoto(int shutterAttempts) {
        if (!TestContext.isAndroid()) {
            throw new UnsupportedOperationException("The emulator camera app is only available on Android");
        }
        elementActions.clickUntilVisible(SystemLocators.androidCameraShutterButton(), SystemLocators.androidCameraDoneButton(),
                shutterAttempts);
        elementActions.click(SystemLocators.androidCameraDoneButton());
    }
}
