package com.dnestr.web.pages;

import com.dnestr.web.actions.ElementActions;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import com.microsoft.playwright.options.AriaRole;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class BasePage {

    protected final Page page;
    protected final ElementActions elementActions;

    protected Locator byRole(AriaRole role, String name) {
        return page.getByRole(role, new Page.GetByRoleOptions().setName(name));
    }

    protected Locator byRole(AriaRole role, String name, boolean exact) {
        return page.getByRole(role, new Page.GetByRoleOptions()
                .setName(name)
                .setExact(exact));
    }

    protected Locator byText(String visibleText) {
        return page.getByText(visibleText);
    }

    public Locator locator(PageElement element) {
        return byRole(element.getRole(), element.getLabel());
    }

    public void click(PageElement button) {
        elementActions.click(locator(button));
    }

    public void type(PageElement field, String text) {
        elementActions.type(locator(field), text);
    }
}