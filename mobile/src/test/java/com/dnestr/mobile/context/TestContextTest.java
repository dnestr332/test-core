package com.dnestr.mobile.context;

import com.dnestr.mobile.enums.MobilePlatform;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestContextTest {

    @AfterEach
    void cleanup() {
        System.clearProperty("platform");
        TestContext.clearPlatform();
    }

    @Test
    void resolvesPlatformWithUnderscoreSuffix() {
        System.setProperty("platform", "ANDROID_14");
        assertThat(TestContext.getPlatform()).isEqualTo(MobilePlatform.ANDROID);
    }

    @Test
    void resolvesPlatformWithoutUnderscore() {
        System.setProperty("platform", "IOS");
        assertThat(TestContext.getPlatform()).isEqualTo(MobilePlatform.IOS);
    }

    @Test
    void missingPropertyThrowsIllegalState() {
        assertThatThrownBy(TestContext::getPlatform)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Platform is not defined");
    }

    @Test
    void invalidPlatformValueThrows() {
        System.setProperty("platform", "WINDOWS_PHONE");
        assertThatThrownBy(TestContext::getPlatform)
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void isAndroidAndIsIosReflectResolvedPlatform() {
        System.setProperty("platform", "ANDROID");
        assertThat(TestContext.isAndroid()).isTrue();
        assertThat(TestContext.isIos()).isFalse();
    }

    @Test
    void isCiMatchesEnvironmentVariable() {
        boolean expected = "true".equalsIgnoreCase(System.getenv("CI"));
        assertThat(TestContext.isCi()).isEqualTo(expected);
    }

    @Test
    void platformIsCachedUntilCleared() {
        System.setProperty("platform", "ANDROID");
        assertThat(TestContext.getPlatform()).isEqualTo(MobilePlatform.ANDROID);

        // property changes without clearing the cache: cached value should still win
        System.setProperty("platform", "IOS");
        assertThat(TestContext.getPlatform()).isEqualTo(MobilePlatform.ANDROID);
    }

    @Test
    void clearPlatformForcesReReadOfSystemProperty() {
        System.setProperty("platform", "ANDROID");
        assertThat(TestContext.getPlatform()).isEqualTo(MobilePlatform.ANDROID);

        TestContext.clearPlatform();
        System.setProperty("platform", "IOS");

        assertThat(TestContext.getPlatform()).isEqualTo(MobilePlatform.IOS);
    }
}
