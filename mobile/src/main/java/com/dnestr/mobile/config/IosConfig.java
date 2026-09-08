package com.dnestr.mobile.config;

import com.dnestr.mobile.enums.MobilePlatform;
import io.appium.java_client.ios.options.XCUITestOptions;

import java.net.URL;

public record IosConfig(
        URL url,
        XCUITestOptions options
) implements DriverConfig {

    @Override
    public MobilePlatform platform() {
        return MobilePlatform.IOS;
    }
}
