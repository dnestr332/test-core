package com.dnestr.mobile.screens;

import com.dnestr.mobile.context.TestContext;
import org.openqa.selenium.By;

/** An element with one locator for Android and another for iOS; the running platform picks one. */
public interface DualLocatorElement extends ScreenElement {

    By getAndroid();

    By getIos();

    @Override
    default By getLocator() {
        return TestContext.isAndroid() ? getAndroid() : getIos();
    }
}
