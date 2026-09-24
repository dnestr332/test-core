package com.dnestr.mobile.pages;

/** Implemented by a screen that has a title element; {@link BaseScreen} provides a default backed by {@link BaseScreen#title()}. */
public interface Screen {

    /** Whether the screen's title element is currently visible. */
    boolean isTitleVisible();

    /** The screen's title element's text. */
    String getTitleText();
}
