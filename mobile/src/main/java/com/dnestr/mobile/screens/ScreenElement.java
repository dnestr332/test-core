package com.dnestr.mobile.screens;

import org.openqa.selenium.By;

/** One element of a screen: a constant of the enum the screen nests, carrying its locator. */
public interface ScreenElement {

    By getLocator();
}
