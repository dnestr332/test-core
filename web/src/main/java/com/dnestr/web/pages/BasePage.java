package com.dnestr.web.pages;

import com.dnestr.base.states.VisibleState;
import com.dnestr.web.actions.ElementActions;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import com.microsoft.playwright.options.AriaRole;
import lombok.RequiredArgsConstructor;

import java.util.regex.Pattern;

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

    /**
     * Locates an element by ARIA role and accessible name (substring match by default; see the 3-arg overload for exact match).
     */
    protected Locator byRole(AriaRole role, String name) {
        return page.getByRole(role, new Page.GetByRoleOptions().setName(name));
    }

    /**
     * Locates an element by ARIA role and accessible name, matching {@code name} exactly rather than as a substring when {@code exact} is {@code true}.
     */
    protected Locator byRole(AriaRole role, String name, boolean exact) {
        return page.getByRole(role, new Page.GetByRoleOptions()
                .setName(name)
                .setExact(exact));
    }

    /**
     * {@code byRole(role, name)}'s Pattern counterpart, for the one case that helper genuinely
     * can't express - a dynamic or regex accessible name. Not for a fixed literal name: use the
     * String overload for that even when it happens to contain regex-special characters.
     */
    protected Locator byRole(AriaRole role, Pattern namePattern) {
        return page.getByRole(role, new Page.GetByRoleOptions().setName(namePattern));
    }

    /**
     * {@link #byRole(AriaRole, String)}'s scoped counterpart: locates an element by ARIA role and
     * accessible name (substring match by default) within {@code within}'s subtree instead of the
     * whole page. Use when the plain, page-wide {@code byRole} could also match an unrelated element
     * elsewhere on the page (e.g. a repeated "Delete" button, one per row of a table).
     */
    protected Locator byRole(Locator within, AriaRole role, String name) {
        return within.getByRole(role, new Locator.GetByRoleOptions().setName(name));
    }

    /**
     * {@link #byRole(AriaRole, String, boolean)}'s scoped counterpart: locates an element by ARIA
     * role and accessible name within {@code within}'s subtree, matching {@code name} exactly
     * rather than as a substring when {@code exact} is {@code true}.
     */
    protected Locator byRole(Locator within, AriaRole role, String name, boolean exact) {
        return within.getByRole(role, new Locator.GetByRoleOptions().setName(name).setExact(exact));
    }

    /**
     * {@link #byRole(AriaRole, Pattern)}'s scoped counterpart: locates an element by ARIA role and
     * a regex accessible name within {@code within}'s subtree instead of the whole page.
     */
    protected Locator byRole(Locator within, AriaRole role, Pattern namePattern) {
        return within.getByRole(role, new Locator.GetByRoleOptions().setName(namePattern));
    }

    /**
     * Locates an element by its visible text content.
     */
    protected Locator byText(String visibleText) {
        return page.getByText(visibleText);
    }

    /**
     * Resolves a {@link PageElement} to its Playwright {@link Locator}, via {@link #byRole} on the element's role and label.
     */
    public Locator locator(PageElement element) {
        return byRole(element.getRole(), element.getLabel());
    }

    /**
     * Clicks {@code button}, resolved via {@link #locator}.
     */
    public void click(PageElement button) {
        elementActions.click(locator(button));
    }

    /**
     * Types {@code text} into {@code field}, resolved via {@link #locator}; replaces any existing content (see {@link ElementActions#type}).
     */
    public void type(PageElement field, String text) {
        elementActions.type(locator(field), text);
    }

    /**
     * Waits up to {@code timeoutMs} for {@code candidates} to resolve to exactly one element, then
     * returns it as-is (still a live {@link Locator}, not resolved to a specific element handle).
     * Use when a locator is expected to be uniquely disambiguated by state that settles
     * asynchronously (e.g. rows filtering down as a search box debounces), where checking
     * {@code candidates.count()} immediately would be flaky.
     *
     * @param candidates the (possibly not-yet-unique) locator to wait on
     * @param description human-readable noun for the element, used only in the failure message
     * @param timeoutMs   maximum time to wait, in milliseconds
     * @return {@code candidates}, once it matches exactly one element
     * @throws IllegalStateException if {@code candidates} still doesn't match exactly one element
     *                                once {@code timeoutMs} elapses
     */
    protected Locator onlyMatch(Locator candidates, String description, int timeoutMs) {
        if (!elementActions.waitForCondition(() -> candidates.count() == 1, timeoutMs)) {
            throw new IllegalStateException(
                    "Expected exactly one %s, found %d".formatted(description, candidates.count())
            );
        }
        return candidates;
    }

    /**
     * Checks {@code text}'s visibility against the expected {@code state}: for {@link VisibleState#VISIBLE},
     * asserts that the first element with that text is currently showing text starting with it; for
     * {@link VisibleState#HIDDEN}, waits up to 3 seconds for it to become hidden or detached. The two
     * branches are intentionally asymmetric — {@code VISIBLE} is a same-instant check (and throws, via
     * {@link ElementActions#text}, if no such element exists at all), while {@code HIDDEN} tolerates
     * text that's still disappearing when this is called.
     *
     * @param text  the visible text to look up via {@link #byText}
     * @param state whether {@code text} is expected to be visible or hidden
     * @return {@code true} if {@code text}'s actual visibility matches {@code state}
     */
    public boolean isTextVisible(String text, VisibleState state) {
        return switch (state) {
            case VISIBLE -> elementActions
                    .text(byText(text).nth(0)).startsWith(text);
            case HIDDEN -> elementActions
                    .isHiddenWithinTimeout(byText(text).nth(0), 3000);
        };
    }
}