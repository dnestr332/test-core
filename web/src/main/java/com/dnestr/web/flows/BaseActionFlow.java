package com.dnestr.web.flows;

import com.dnestr.web.pages.AppPage;
import com.dnestr.web.pages.PageElement;
import com.dnestr.web.pages.BasePage;
import com.dnestr.web.resolvers.BasePageResolver;
import lombok.RequiredArgsConstructor;

/**
 * Base for flow classes that perform actions across one or more pages without callers having to
 * resolve a {@link BasePage} themselves. Subclasses take a page enum + {@link PageElement} pair —
 * {@link #resolve} looks up the matching {@link BasePage} via the injected {@link BasePageResolver}
 * — and add whatever multi-step, multi-page methods a scenario needs (e.g. "log in", "submit
 * checkout") on top of the single-page {@link #click}/{@link #type} primitives below.
 * <p>
 * Meant to be extended, one flow subclass per logical user journey; keep page/element-agnostic
 * plumbing here and put the specific pages/elements/order-of-steps in the subclass.
 *
 * @param <P> the app's page enum, which must both be an {@code enum} and implement {@link AppPage}
 */
@RequiredArgsConstructor
public abstract class BaseActionFlow<P extends Enum<P> & AppPage> {

    private final BasePageResolver<P> pageResolver;

    /** Resolves the {@link BasePage} instance backing {@code page} via the injected {@link BasePageResolver}. */
    protected BasePage resolve(P page) {
        return pageResolver.resolvePage(page);
    }

    /** Clicks {@code element} on {@code page}. */
    public void click(PageElement element, P page) {
        resolve(page).click(element);
    }

    /** Types {@code value} into {@code element} on {@code page}, replacing any existing content. */
    public void type(PageElement element, P page, String value) {
        resolve(page).type(element, value);
    }
}