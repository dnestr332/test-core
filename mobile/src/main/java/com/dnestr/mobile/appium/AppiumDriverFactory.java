package com.dnestr.mobile.appium;

import com.dnestr.mobile.config.AndroidConfig;
import com.dnestr.mobile.config.IosConfig;
import com.dnestr.mobile.enums.MobilePlatform;
import com.dnestr.mobile.config.DriverConfig;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import lombok.extern.slf4j.Slf4j;

import static com.dnestr.base.logs.LogStyles.*;

/** Builds the concrete {@link AppiumDriver} subclass matching a {@link DriverConfig}'s platform. */
@Slf4j
public class AppiumDriverFactory {

    /**
     * Creates an {@link AndroidDriver} or {@link IOSDriver} depending on {@code config}'s concrete
     * type, connecting to {@code config.url()} with its platform-specific options.
     *
     * @throws IllegalStateException if {@code config} is neither {@link AndroidConfig} nor {@link IosConfig}
     */
    public AppiumDriver createDriver(DriverConfig config) {
        MobilePlatform platform = config.platform();

        log.info("{} {} Creating AppiumDriver for platform: {}{}",
                INFO_SHORT, INFO, GREEN + platform, RESET);

        return switch (config) {
            case AndroidConfig android -> new AndroidDriver(
                    android.url(),
                    android.options());
            case IosConfig ios -> new IOSDriver(
                    ios.url(),
                    ios.options());

            default -> throw new IllegalStateException("Unexpected config: " + config);
        };
    }
}
