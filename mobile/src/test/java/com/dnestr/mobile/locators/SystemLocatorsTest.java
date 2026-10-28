package com.dnestr.mobile.locators;

import io.appium.java_client.AppiumBy;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static org.assertj.core.api.Assertions.assertThat;

class SystemLocatorsTest {

    @Test
    void androidAlertButtonMatchesTheLabelIgnoringCase() {
        assertThat(SystemLocators.androidAlertButton("Ok")).isEqualTo(By.xpath(
                "//*[starts-with(@resource-id,'android:id/button')]"
                        + "[translate(@text,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')='OK']"));
    }

    @Test
    void androidAlertButtonKeepsAnApostropheInTheLabel() {
        assertThat(SystemLocators.androidAlertButton("Don't allow").toString()).contains("=\"DON'T ALLOW\"]");
    }

    @Test
    void androidNotificationIsFoundByItsText() {
        assertThat(SystemLocators.androidNotification("New ride")).isEqualTo(By.xpath("//*[contains(@text,'New ride')]"));
    }

    @Test
    void iosAlertButtonIsAButtonOfTheAlert() {
        assertThat(SystemLocators.iosAlertButton("Allow"))
                .isEqualTo(By.xpath("//XCUIElementTypeAlert//XCUIElementTypeButton[@name='Allow']"));
    }

    @Test
    void cameraButtonsMatchTheirIdSuffix() {
        assertThat(SystemLocators.androidCameraShutterButton())
                .isEqualTo(By.xpath("//*[contains(@resource-id,':id/shutter_button')]"));
        assertThat(SystemLocators.androidCameraDoneButton())
                .isEqualTo(By.xpath("//*[contains(@resource-id,':id/done_button')]"));
    }

    @Test
    void iosNotificationMatchesThePartOfTheLabelIgnoringCase() {
        assertThat(SystemLocators.iosNotification("Please review"))
                .isEqualTo(AppiumBy.iOSNsPredicateString("label CONTAINS[c] 'Please review'"));
    }

    @Test
    void iosNotificationEscapesQuotesInThePredicate() {
        assertThat(SystemLocators.iosNotification("Driver's documents"))
                .isEqualTo(AppiumBy.iOSNsPredicateString("label CONTAINS[c] 'Driver\\'s documents'"));
    }
}
