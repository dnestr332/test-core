package com.dnestr.mobile.context;

import com.dnestr.base.utils.EnumUtils;
import com.dnestr.mobile.enums.MobilePlatform;

/**
 * Thread-local holder for the current test thread's target {@link MobilePlatform} and CI/local
 * detection, read throughout the mobile module (e.g. {@code MobileElementUtils},
 * {@code MobileValidationUtils}, {@code MobileDeviceUtils}) to branch platform-specific behavior
 * without threading a platform parameter through every call.
 */
public final class TestContext {

    private static final ThreadLocal<MobilePlatform> PLATFORM = new ThreadLocal<>();

    private TestContext() {}

    /**
     * Returns the current thread's {@link MobilePlatform}, resolving and caching it on first call
     * from the {@code platform} system property (e.g. {@code -Dplatform=android_phone} resolves to
     * {@code ANDROID} — everything up to the first {@code _} is parsed; a device/form-factor suffix
     * is allowed and ignored).
     *
     * @throws IllegalStateException if the {@code platform} system property isn't set, or its
     *                                (prefix of the) value doesn't match a {@link MobilePlatform} constant
     */
    public static MobilePlatform getPlatform() {
        MobilePlatform current = PLATFORM.get();
        if (current == null) {
            String raw = System.getProperty("platform");

            if (raw == null) throw new IllegalStateException("Platform is not defined");

            int separator = raw.indexOf("_");
            String actual = separator >= 0 ? raw.substring(0, separator) : raw;
            current = EnumUtils.parse(MobilePlatform.class, actual);
            PLATFORM.set(current);
        }
        return current;
    }

    /** Clears the current thread's cached platform, so the next {@link #getPlatform()} call re-resolves it from the system property. */
    public static void clearPlatform() {
        PLATFORM.remove();
    }

    /** Whether the {@code CI} environment variable is set to {@code "true"} (case-insensitive). */
    public static boolean isCi() {
        return "true".equalsIgnoreCase(System.getenv("CI"));
    }

    /** Whether the current thread's platform ({@link #getPlatform()}) is {@link MobilePlatform#ANDROID}. */
    public static boolean isAndroid() {
        return getPlatform() == MobilePlatform.ANDROID;
    }

    /** Whether the current thread's platform ({@link #getPlatform()}) is {@link MobilePlatform#IOS}. */
    public static boolean isIos() {
        return getPlatform() == MobilePlatform.IOS;
    }
}
