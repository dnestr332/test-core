package com.dnestr.mobile.pages;

/** Implemented by a screen that has a dismiss/close action (e.g. a modal or dialog screen); {@link BaseScreen} provides a default backed by {@link BaseScreen#closeButton()}. */
public interface Closable {

    /** Dismisses the screen. */
    void close();
}
