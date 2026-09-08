package com.dnestr.web.flows;

import com.dnestr.web.pages.AppPage;
import com.dnestr.web.pages.PageElement;
import com.dnestr.web.pages.BasePage;
import com.dnestr.web.resolvers.BasePageResolver;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class BaseActionFlow<P extends Enum<P> & AppPage> {

    private final BasePageResolver<P> pageResolver;

    protected BasePage resolve(P page) {
        return pageResolver.resolvePage(page);
    }

    public void click(PageElement element, P page) {
        resolve(page).click(element);
    }

    public void type(PageElement element, P page, String value) {
        resolve(page).type(element, value);
    }
}