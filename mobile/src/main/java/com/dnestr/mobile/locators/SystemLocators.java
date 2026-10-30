package com.dnestr.mobile.locators;

import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

import java.util.Locale;

import static com.dnestr.mobile.locators.XPathUtils.literal;

/**
 * The OS's own UI an app hands off to — permission dialogs, alerts, the notification shade, the camera app. It has
 * system resource ids or plain text rather than test ids. Ids are matched by their {@code :id/...} suffix, since the
 * package that draws them differs between Android versions and vendors.
 */
public final class SystemLocators {

    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private SystemLocators() {}

    /** An element whose resource-id contains {@code idPart}, e.g. {@code ":id/shutter_button"}. */
    public static By byAndroidResourceIdContaining(String idPart) {
        return By.xpath("//*[contains(@resource-id,%s)]".formatted(literal(idPart)));
    }

    /** The runtime-permission dialog's allow button: "While using the app" where Android offers it, otherwise "Allow". */
    public static By androidPermissionAllowButton() {
        return By.xpath("//*[contains(@resource-id,':id/permission_allow_foreground_only_button')"
                + " or contains(@resource-id,':id/permission_allow_button')]"
        );
    }

    /** The runtime-permission dialog's deny button ("Don't allow"). */
    public static By androidPermissionDenyButton() {
        return byAndroidResourceIdContaining(":id/permission_deny_button");
    }

    /** A notification in the open shade whose text contains {@code text}; the shade's rows have no ids of their own. */
    public static By androidNotification(String text) {
        return By.xpath("//*[contains(@text,%s)]".formatted(literal(text)));
    }

    /** A notification in Notification Center, or a banner on screen, whose label contains {@code text}, ignoring case. */
    public static By iosNotification(String text) {
        String quoted = text.replace("\\", "\\\\").replace("'", "\\'");
        return AppiumBy.iOSNsPredicateString("label CONTAINS[c] '%s'".formatted(quoted));
    }

    /** A button of a native alert dialog, by its label ignoring case: Material dialogs draw labels in capitals. */
    public static By androidAlertButton(String label) {
        return By.xpath("//*[starts-with(@resource-id,'android:id/button')][translate(@text,'%s','%s')=%s]"
                .formatted(LOWER, UPPER, literal(label.toUpperCase(Locale.ROOT)))
        );
    }

    /** The alert on screen. */
    public static By iosAlert() {
        return By.xpath("//XCUIElementTypeAlert");
    }

    /** The title of the alert on screen: its first text. */
    public static By iosAlertTitle() {
        return By.xpath("(//XCUIElementTypeAlert//XCUIElementTypeStaticText)[1]");
    }

    /** The message of the alert on screen: the text under its title. */
    public static By iosAlertMessage() {
        return By.xpath("(//XCUIElementTypeAlert//XCUIElementTypeStaticText)[2]");
    }

    /** A button of the alert on screen, by its label. */
    public static By iosAlertButton(String label) {
        return By.xpath("//XCUIElementTypeAlert//XCUIElementTypeButton[@name=%s]".formatted(literal(label)));
    }

    /** The shutter of the emulator's camera app. */
    public static By androidCameraShutterButton() {
        return byAndroidResourceIdContaining(":id/shutter_button");
    }

    /** The camera app's confirm button, shown once a photo is taken. */
    public static By androidCameraDoneButton() {
        return byAndroidResourceIdContaining(":id/done_button");
    }

    /** The keyboard's Return or Done key, which ends editing on a single-line field; Next and Go are left out, as they move focus or submit. */
    public static By iosKeyboardDismissKey() {
        return AppiumBy.iOSClassChain(
                "**/XCUIElementTypeKeyboard/**/XCUIElementTypeButton[`name IN {'Return', 'return', 'Done', 'done'}`]"
        );
    }

    /** Done on the keyboard's toolbar, which a number pad needs to close. */
    public static By iosKeyboardDoneButton() {
        return AppiumBy.iOSNsPredicateString(
                "name == 'Done' AND type IN {'XCUIElementTypeButton', 'XCUIElementTypeStaticText'}"
        );
    }
}
