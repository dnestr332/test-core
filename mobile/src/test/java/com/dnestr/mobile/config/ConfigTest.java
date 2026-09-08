package com.dnestr.mobile.config;

import com.dnestr.mobile.enums.MobilePlatform;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.options.XCUITestOptions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigTest {

    @Test
    void androidConfigReportsAndroidPlatform() throws Exception {
        URL url = URI.create("http://localhost:4723").toURL();
        AndroidConfig config = new AndroidConfig(url, new UiAutomator2Options());

        assertThat(config.platform()).isEqualTo(MobilePlatform.ANDROID);
        assertThat(config.url()).isEqualTo(url);
    }

    @Test
    void iosConfigReportsIosPlatform() throws Exception {
        URL url = URI.create("http://localhost:4723").toURL();
        IosConfig config = new IosConfig(url, new XCUITestOptions());

        assertThat(config.platform()).isEqualTo(MobilePlatform.IOS);
        assertThat(config.url()).isEqualTo(url);
    }

    @Test
    void waitConfigUsesCiValuesWhenCiFlagTrue() {
        WaitConfig config = WaitConfig.of(10, 30, 5, 15, 200, true);

        assertThat(config.longTimeout()).isEqualTo(30);
        assertThat(config.shortTimeout()).isEqualTo(15);
        assertThat(config.polling()).isEqualTo(200);
    }

    @Test
    void waitConfigUsesLocalValuesWhenCiFlagFalse() {
        WaitConfig config = WaitConfig.of(10, 30, 5, 15, 200, false);

        assertThat(config.longTimeout()).isEqualTo(10);
        assertThat(config.shortTimeout()).isEqualTo(5);
        assertThat(config.polling()).isEqualTo(200);
    }
}
