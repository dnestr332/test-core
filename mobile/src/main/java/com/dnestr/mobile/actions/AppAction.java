package com.dnestr.mobile.actions;

/** App lifecycle action for {@code MobileDeviceUtils#controlApp}: kill the app, bring it to the foreground, or kill-then-relaunch it. */
public enum AppAction {

    TERMINATE,
    ACTIVATE,
    RESTART
}
