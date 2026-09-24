package com.dnestr.web.flows;

import com.dnestr.base.assertions.Softly;
import com.dnestr.base.states.*;
import com.dnestr.web.actions.ElementActions;
import com.dnestr.web.assertions.Hardly;
import com.dnestr.web.pages.AppPage;
import com.dnestr.web.pages.PageElement;
import com.dnestr.web.pages.BasePage;
import com.dnestr.web.resolvers.BasePageResolver;
import com.microsoft.playwright.Locator;
import lombok.RequiredArgsConstructor;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Base for flow classes that assert on page state via a page enum + {@link PageElement}, rather
 * than requiring callers to resolve a {@link BasePage} and locator themselves. Every {@code verify*}
 * method takes an {@link AssertionState}: {@code STRICTLY} routes to {@link Hardly} (fails the test
 * immediately), {@code SOFTLY} routes to {@code com.dnestr.base.assertions.Softly} (accumulates the
 * failure for a later {@code Softly.assertAll()}) — same check, different failure semantics, chosen
 * by the caller per assertion.
 * <p>
 * <b>None of the checks below wait or retry.</b> Each condition — {@code locator.isVisible()},
 * {@code elementActions.text(locator)}, etc. — is read exactly once, synchronously, before being
 * handed to {@link Hardly}/{@code Softly}, neither of which polls (see {@link Hardly}'s class docs).
 * That's different from Playwright's own web-first assertions (e.g. {@link Hardly#isVisible}),
 * which retry until a timeout. Calling a {@code verify*} method immediately after an action that
 * triggers async UI change (navigation, a fetch-driven re-render) can therefore catch the UI
 * mid-transition and fail — or, for a {@code HIDDEN}/{@code UNCHECKED}-style check, pass
 * incorrectly on stale state. Wait for the UI to settle first (e.g.
 * {@code elementActions.waitForStability()}, or a targeted {@code isVisibleWithinTimeout}) before
 * asserting through this class.
 *
 * @param <P> the app's page enum, which must both be an {@code enum} and implement {@link AppPage}
 */
@RequiredArgsConstructor
public abstract class BaseAssertionFlow<P extends Enum<P> & AppPage> {

    private final BasePageResolver<P> pageResolver;
    private final ElementActions elementActions;

    /** Resolves the {@link BasePage} instance backing {@code page} via the injected {@link BasePageResolver}. */
    protected BasePage resolve(P page) {
        return pageResolver.resolvePage(page);
    }

    /** Evaluates {@code condition} once and routes the result to {@link Hardly#isTrue} or {@code Softly#isTrue} per {@code state}. No retry — see the class docs. */
    private void assertBool(AssertionState state, BooleanSupplier condition, String message) {
        switch (state) {
            case STRICTLY -> Hardly.isTrue(condition.getAsBoolean(), message);
            case SOFTLY -> Softly.isTrue(condition.getAsBoolean(), message);
        }
    }

    /** Evaluates {@code actual} once and routes the comparison to {@link Hardly#isEqual} or {@code Softly#isEqual} per {@code state}. No retry — see the class docs. */
    private <T> void assertEquals(AssertionState state, Supplier<T> actual, T expected, String message) {
        T value = actual.get();

        switch (state) {
            case STRICTLY -> Hardly.isEqual(value, expected, message);
            case SOFTLY -> Softly.isEqual(value, expected, message);
        }
    }

    /**
     * Asserts whether {@code element} on {@code page} is visible, per {@code visible}. A locator
     * that matches zero elements counts as not visible (rather than throwing), so this also covers
     * elements that haven't been rendered/attached at all.
     */
    public void verifyVisibleState(P page, PageElement element, AssertionState assertion, VisibleState visible) {
        Locator locator = resolve(page).locator(element);

        BooleanSupplier condition = switch (visible) {
            case VISIBLE -> () -> locator.count() > 0 && locator.first().isVisible();
            case HIDDEN -> () -> locator.count() == 0 || !locator.first().isVisible();
        };

        String msg = "%s should be %s".formatted(element, visible.name().replace("_", " ").toLowerCase());
        assertBool(assertion, condition, msg);
    }

    /** Asserts whether {@code element} on {@code page} is enabled or disabled, per {@code state}. */
    public void verifyButtonState(P page, PageElement element, AssertionState assertion, ButtonState state) {
        Locator locator = resolve(page).locator(element);

        BooleanSupplier condition = switch (state) {
            case ENABLED -> locator::isEnabled;
            case DISABLED -> locator::isDisabled;
        };

        String msg = "%s should be %s".formatted(element, state);
        assertBool(assertion, condition, msg);
    }

    /** Asserts whether {@code element} on {@code page} is editable, per {@code state}; a disabled field counts as {@code READ_ONLY}. */
    public void verifyFieldState(P page, PageElement element, AssertionState assertion, FieldState state) {
        Locator locator = resolve(page).locator(element);
        BooleanSupplier condition = switch (state) {
            case EDITABLE -> locator::isEditable;
            case READ_ONLY -> () -> locator.isDisabled() || !locator.isEditable();
        };

        String msg = "%s should be %s".formatted(element, state.name().replace("_", " ").toLowerCase());
        assertBool(assertion, condition, msg);
    }

    /** Asserts whether {@code element} (a checkbox/radio/switch) on {@code page} is checked, per {@code state}. */
    public void verifyToggleState(P page, PageElement element, AssertionState assertion, ToggleState state) {
        Locator locator = resolve(page).locator(element);
        BooleanSupplier condition = switch (state) {
            case CHECKED -> locator::isChecked;
            case UNCHECKED -> () -> !locator.isChecked();
        };

        String msg = "Toggle should be " + state.name().toLowerCase();
        assertBool(assertion, condition, msg);
    }

    /**
     * Asserts {@code element} on {@code page} has text exactly equal to {@code expected}, via
     * {@link ElementActions#text}. Curly right-single-quotes ({@code ’}) in the actual text are
     * normalized to a plain apostrophe before comparing, so {@code expected} can be written with
     * a plain {@code '} regardless of which one the rendered page happens to use.
     */
    public void verifyTextEquals(P page, PageElement element, AssertionState assertion, String expected) {
        Locator locator = resolve(page).locator(element);
        Supplier<String> actual = () -> elementActions
                .text(locator)
                .replace("’", "'");

        String msg = "%s text should be <%s>".formatted(element, expected);
        assertEquals(assertion, actual, expected, msg);
    }

    /** Generic escape hatch: asserts {@code actual.get()} equals {@code expected}, dispatched hard/soft per {@code assertion}, for checks that don't fit the page/element-based methods above. */
    public <T> void verifyEquals(AssertionState assertion, Supplier<T> actual, T expected, String context) {
        assertEquals(assertion, actual, expected, context);
    }

    /** Asserts {@code element} on {@code page} has text containing {@code partial} as a substring, via {@link ElementActions#text}. */
    public void verifyTextContains(P page, PageElement element, AssertionState assertion, String partial) {
        Locator locator = resolve(page).locator(element);
        String actual = elementActions.text(locator);
        BooleanSupplier condition = () -> actual.contains(partial);

        String msg = "%s should contain <%s>, but was <%s>".formatted(element, partial, actual);
        assertBool(assertion, condition, msg);
    }
}