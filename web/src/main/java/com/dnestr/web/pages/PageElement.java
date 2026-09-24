package com.dnestr.web.pages;

import com.microsoft.playwright.options.AriaRole;

/**
 * Marker implemented by an app's element enum (one constant per interactive element on a page)
 * describing how to find it via ARIA role — {@link BasePage#locator} turns this pair into a
 * Playwright {@code Locator} via {@code page.getByRole(role, options.setName(label))}. Elements
 * that can't be found this way (e.g. by test id or CSS) don't implement this interface; page
 * objects reach for {@link BasePage#byText} or their own {@code Locator}-returning methods instead.
 */
public interface PageElement {

    /** The accessible name Playwright matches against (the {@code name} option of {@code getByRole}). */
    String getLabel();

    /** The element's ARIA role, e.g. {@code AriaRole.BUTTON}. */
    AriaRole getRole();
}
