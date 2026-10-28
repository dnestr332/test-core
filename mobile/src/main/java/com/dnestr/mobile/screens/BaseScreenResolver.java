package com.dnestr.mobile.screens;

/**
 * Looks up the {@link BaseScreen} instance for a {@link AppScreen} constant — mobile's counterpart to the web
 * module's {@code BasePageResolver}. Implemented once per app, e.g. from a dependency-injection container.
 */
public interface BaseScreenResolver {

    /**
     * Returns the {@link BaseScreen} that draws {@code screen}.
     *
     * @throws IllegalArgumentException if {@code screen} has no screen class wired
     */
    BaseScreen resolveScreen(AppScreen screen);
}
