package com.dnestr.mobile.context;

import com.dnestr.core.utils.EnumUtils;
import com.dnestr.mobile.enums.MobilePlatform;

public final class TestContext {

    private static final ThreadLocal<MobilePlatform> PLATFORM = new ThreadLocal<>();

    private TestContext() {}

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

    public static void clearPlatform() {
        PLATFORM.remove();
    }

    public static boolean isCi() {
        return "true".equalsIgnoreCase(System.getenv("CI"));
    }

    public static boolean isAndroid() {
        return getPlatform() == MobilePlatform.ANDROID;
    }

    public static boolean isIos() {
        return getPlatform() == MobilePlatform.IOS;
    }
}
