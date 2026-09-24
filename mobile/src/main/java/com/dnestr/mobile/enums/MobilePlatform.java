package com.dnestr.mobile.enums;

/** The mobile OS a test run targets, read via {@code TestContext#getPlatform()} to branch platform-specific behavior (e.g. in {@code MobileElementUtils}, {@code MobileValidationUtils}). */
public enum MobilePlatform {

    ANDROID,
    IOS
}
