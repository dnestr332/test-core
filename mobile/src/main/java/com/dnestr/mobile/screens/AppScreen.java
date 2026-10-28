package com.dnestr.mobile.screens;

/**
 * Implemented by an app's screen enum (one constant per screen state), naming the {@link BaseScreen} class that
 * draws it. A {@link BaseScreenResolver} turns the constant into that screen's instance, so flows and steps take the
 * enum rather than having every screen object injected.
 */
public interface AppScreen {

    /** The screen object class for this state; {@code null} when none is wired yet. */
    Class<? extends BaseScreen> getScreenClass();
}
