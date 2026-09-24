package com.dnestr.mobile.config;

import com.dnestr.mobile.enums.MobilePlatform;

import java.net.URL;

/**
 * Common shape of a platform-specific Appium driver configuration, implemented by
 * {@link AndroidConfig} and {@link IosConfig}. {@code AppiumDriverFactory#createDriver} pattern-
 * matches on the concrete type to pick which {@code AppiumDriver} subclass to construct.
 */
public interface DriverConfig {

    /** Which platform this config targets. */
    MobilePlatform platform();

    /** The Appium server URL to connect to. */
    URL url();
}
