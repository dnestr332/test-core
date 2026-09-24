package com.dnestr.web.pages;

/**
 * Marker implemented by an app's page enum (one constant per page/screen) so it can be used as the
 * {@code P extends Enum<P> & AppPage} type parameter throughout this framework (e.g.
 * {@code BasePageResolver<P>}, {@code BaseActionFlow<P>}, {@code BaseAssertionFlow<P>}) — flows and
 * resolvers key off the enum constant rather than a {@link BasePage} instance directly, so a page
 * only needs to be resolved when actually navigated to.
 */
public interface AppPage {

    /** The page's route/path, e.g. for building a URL or asserting on the current location after navigation. */
    String getPath();
}
