package com.dnestr.web.resolvers;

import com.dnestr.web.pages.BasePage;
import com.dnestr.web.pages.AppPage;

/**
 * Looks up the {@link BasePage} instance for a page enum constant. Implemented once per app (e.g.
 * backed by a {@code Map<P, BasePage>} or a {@code switch} over the enum) and injected into
 * {@code BaseActionFlow}/{@code BaseAssertionFlow}, which is how those flow classes turn a page
 * enum + {@link com.dnestr.web.pages.PageElement} pair into an actual Playwright interaction
 * without needing every page object injected into them directly.
 *
 * @param <P> the app's page enum, which must both be an {@code enum} and implement {@link AppPage}
 */
public interface BasePageResolver<P extends Enum<P> & AppPage> {

    /** Returns the {@link BasePage} that backs {@code page}. */
    BasePage resolvePage(P page);
}
