package com.dnestr.web.resolvers;

import com.dnestr.web.pages.AppPage;
import com.dnestr.web.pages.PageElement;

/**
 * Resolves the free-form element name from a BDD step (e.g. {@code "Submit button"}) to the
 * matching {@link PageElement} constant for the given page. Implemented once per app — typically
 * as a lookup over that page's {@link PageElement} enum(s) — so scenarios can refer to elements by
 * a human-readable label rather than a Java constant name. {@code test-core} ships no
 * implementation; each consuming app implements it and wires it into its step-definition layer.
 *
 * @param <P> the app's page enum, which must both be an {@code enum} and implement {@link AppPage}
 */
public interface BaseElementResolver<P extends Enum<P> & AppPage> {

    /** Returns the {@link PageElement} on {@code page} identified by {@code raw}. */
    PageElement resolveElement(P page, String raw);
}
