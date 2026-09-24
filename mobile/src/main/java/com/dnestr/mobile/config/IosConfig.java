package com.dnestr.mobile.config;

import com.dnestr.mobile.enums.MobilePlatform;
import io.appium.java_client.ios.options.XCUITestOptions;

import java.net.URL;

/** {@link DriverConfig} for iOS, pairing the Appium server URL with XCUITest-specific capabilities. */
public record IosConfig(
        URL url,
        XCUITestOptions options
) implements DriverConfig {

    @Override
    public MobilePlatform platform() {
        return MobilePlatform.IOS;
    }
}
