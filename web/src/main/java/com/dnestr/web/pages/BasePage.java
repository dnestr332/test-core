package com.dnestr.web.pages;

import com.dnestr.web.actions.ElementActions;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import com.microsoft.playwright.options.AriaRole;
import lombok.RequiredArgsConstructor;

/**
 * Base for a single page's page object. Holds the Playwright {@link Page} and the shared
 * {@link ElementActions} instance (passed in by the subclass constructor via
 * {@link RequiredArgsConstructor} — this class is used by composition/injection, not extended by
 * {@code ElementActions} itself), and provides the common {@link PageElement} → {@link Locator}
 * lookup plus the two most frequently used actions. Subclasses add page-specific locator methods
 * and higher-level page behavior on top; anything generic and Playwright-only belongs here instead.
 */
@RequiredArgsConstructor
public abstract class BasePage {

    protected final Page page;
    protected final ElementActions elementActions;

    /** Locates an element by ARIA role and accessible name (substring match by default; see the 3-arg overload for exact match). */
    protected Locator byRole(AriaRole role, String name) {
        return page.getByRole(role, new Page.GetByRoleOptions().setName(name));
    }

    /** Locates an element by ARIA role and accessible name, matching {@code name} exactly rather than as a substring when {@code exact} is {@code true}. */
    protected Locator byRole(AriaRole role, String name, boolean exact) {
        return page.getByRole(role, new Page.GetByRoleOptions()
                .setName(name)
                .setExact(exact));
    }

    /** Locates an element by its visible text content. */
    protected Locator byText(String visibleText) {
        return page.getByText(visibleText);
    }

    /** Resolves a {@link PageElement} to its Playwright {@link Locator}, via {@link #byRole} on the element's role and label. */
    public Locator locator(PageElement element) {
        return byRole(element.getRole(), element.getLabel());
    }

    /** Clicks {@code button}, resolved via {@link #locator}. */
    public void click(PageElement button) {
        elementActions.click(locator(button));
    }

    /** Types {@code text} into {@code field}, resolved via {@link #locator}; replaces any existing content (see {@link ElementActions#type}). */
    public void type(PageElement field, String text) {
        elementActions.type(locator(field), text);
    }
}