package com.dnestr.mobile.utils;

import com.dnestr.mobile.context.TestContext;
import lombok.experimental.UtilityClass;

/** Refuses a platform-only call on the other platform, naming what was asked for. */
@UtilityClass
class PlatformGuards {

    void requireAndroid(String what) {
        if (!TestContext.isAndroid()) {
            throw new UnsupportedOperationException(what + " is only available on Android");
        }
    }

    void requireIos(String what) {
        if (!TestContext.isIos()) {
            throw new UnsupportedOperationException(what + " is only available on iOS");
        }
    }
}
