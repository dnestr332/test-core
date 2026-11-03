package com.dnestr.mobile.screens;

import com.dnestr.mobile.context.TestContext;
import io.appium.java_client.AppiumBy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static org.assertj.core.api.Assertions.assertThat;

/** A dual-locator element hands over the running platform's locator. */
class ScreenElementTest {

    @Getter
    @RequiredArgsConstructor
    enum NativeElement implements DualLocatorElement {
        NEXT_BUTTON(By.id("app:id/next"), AppiumBy.accessibilityId("Next"));

        private final By android;
        private final By ios;
    }

    @AfterEach
    void cleanup() {
        System.clearProperty("platform");
        TestContext.clearPlatform();
    }

    @Test
    void aDualLocatorElementResolvesToTheRunningPlatformsLocator() {
        System.setProperty("platform", "ios");
        assertThat(NativeElement.NEXT_BUTTON.getLocator()).isEqualTo(AppiumBy.accessibilityId("Next"));

        TestContext.clearPlatform();
        System.setProperty("platform", "android");
        assertThat(NativeElement.NEXT_BUTTON.getLocator()).isEqualTo(By.id("app:id/next"));
    }
}
