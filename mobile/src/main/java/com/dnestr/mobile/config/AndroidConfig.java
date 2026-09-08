package com.dnestr.mobile.config;

import com.dnestr.mobile.enums.MobilePlatform;
import io.appium.java_client.android.options.UiAutomator2Options;

import java.net.URL;

public record AndroidConfig(
        URL url,
        UiAutomator2Options options
) implements DriverConfig {

    @Override
    public MobilePlatform platform() {
        return MobilePlatform.ANDROID;
    }
}
