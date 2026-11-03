package com.dnestr.mobile.context;

/** The mobile OS a test run targets, read via {@code TestContext#getPlatform()} to branch platform-specific behavior (e.g. in {@code ElementUtils}, {@code ValidationUtils}). */
public enum MobilePlatform {

    ANDROID,
    IOS
}
