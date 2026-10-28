package com.dnestr.mobile.utils;

import com.dnestr.mobile.context.TestContext;

/** Refuses a platform-only call on the other platform, naming what was asked for. */
final class PlatformGuards {

    private PlatformGuards() {}

    static void requireAndroid(String what) {
        if (!TestContext.isAndroid()) {
            throw new UnsupportedOperationException(what + " is only available on Android");
        }
    }

    static void requireIos(String what) {
        if (!TestContext.isIos()) {
            throw new UnsupportedOperationException(what + " is only available on iOS");
        }
    }
}
