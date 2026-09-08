package com.dnestr.web.resolvers;

import com.dnestr.web.pages.BasePage;
import com.dnestr.web.pages.AppPage;

public interface BasePageResolver<P extends Enum<P> & AppPage> {

    BasePage resolvePage(P page);
}
