package com.dnestr.web.resolvers;

import com.dnestr.web.pages.AppPage;
import com.dnestr.web.pages.PageElement;

public interface BaseElementResolver<P extends Enum<P> & AppPage> {

    PageElement resolveElement(P page, String raw);
}
