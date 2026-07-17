package com.dnestr.web.flows;

import com.dnestr.core.assertions.Softly;
import com.dnestr.core.states.AssertionState;
import com.dnestr.core.states.ButtonState;
import com.dnestr.core.states.FieldState;
import com.dnestr.core.states.ToggleState;
import com.dnestr.core.states.VisibleState;
import com.dnestr.web.actions.ElementActions;
import com.dnestr.web.pages.AppPage;
import com.dnestr.web.pages.BasePage;
import com.dnestr.web.pages.PageElement;
import com.dnestr.web.resolvers.BasePageResolver;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BaseAssertionFlowTest {

    enum TestPage implements AppPage {
        HOME;

        @Override
        public String getPath() {
            return "/home";
        }
    }

    private static final PageElement ELEMENT = new PageElement() {
        @Override
        public String getLabel() {
            return "Submit";
        }

        @Override
        public AriaRole getRole() {
            return AriaRole.BUTTON;
        }
    };

    @Mock
    private BasePageResolver<TestPage> pageResolver;
    @Mock
    private BasePage basePage;
    @Mock
    private ElementActions elementActions;
    @Mock
    private Locator locator;

    private BaseAssertionFlow<TestPage> flow;

    @BeforeEach
    void setUp() {
        flow = new BaseAssertionFlow<>(pageResolver, elementActions) {};
        lenient().when(pageResolver.resolvePage(TestPage.HOME)).thenReturn(basePage);
        lenient().when(basePage.locator(ELEMENT)).thenReturn(locator);
    }

    @AfterEach
    void tearDown() {
        Softly.reset();
    }

    @Test
    void verifyVisibleState_VISIBLE_passes_whenLocatorPresentAndVisible() {
        when(locator.count()).thenReturn(1);
        when(locator.first()).thenReturn(locator);
        when(locator.isVisible()).thenReturn(true);

        assertThatNoException().isThrownBy(() ->
                flow.verifyVisibleState(TestPage.HOME, ELEMENT, AssertionState.STRICTLY, VisibleState.VISIBLE));
    }

    @Test
    void verifyVisibleState_VISIBLE_isSnapshotCheck_notPlaywrightAutoRetrying() {
        // Known inconsistency (flagged in review, left as-is -- may be a deliberate
        // design choice): this branch reads locator.count() > 0 && first().isVisible()
        // once, rather than delegating to Playwright's auto-retrying assertThat(locator)
        // .isVisible() the way Hardly.isVisible() does. This test locks down the current
        // one-shot semantics rather than the auto-retrying alternative.
        when(locator.count()).thenReturn(0);

        assertThatThrownBy(() ->
                flow.verifyVisibleState(TestPage.HOME, ELEMENT, AssertionState.STRICTLY, VisibleState.VISIBLE))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void verifyVisibleState_NOT_VISIBLE_passes_whenCountIsZero() {
        when(locator.count()).thenReturn(0);

        assertThatNoException().isThrownBy(() ->
                flow.verifyVisibleState(TestPage.HOME, ELEMENT, AssertionState.STRICTLY, VisibleState.NOT_VISIBLE));
    }

    @Test
    void verifyVisibleState_SOFTLY_recordsFailure_withoutThrowingImmediately() {
        when(locator.count()).thenReturn(0);

        assertThatNoException().isThrownBy(() ->
                flow.verifyVisibleState(TestPage.HOME, ELEMENT, AssertionState.SOFTLY, VisibleState.VISIBLE));

        assertThatThrownBy(Softly::assertAll).isInstanceOf(AssertionError.class);
    }

    @Test
    void verifyButtonState_ENABLED_delegatesToLocatorIsEnabled() {
        when(locator.isEnabled()).thenReturn(true);

        assertThatNoException().isThrownBy(() ->
                flow.verifyButtonState(TestPage.HOME, ELEMENT, AssertionState.STRICTLY, ButtonState.ENABLED));
    }

    @Test
    void verifyButtonState_DISABLED_failsStrictly_whenNotDisabled() {
        when(locator.isDisabled()).thenReturn(false);

        assertThatThrownBy(() ->
                flow.verifyButtonState(TestPage.HOME, ELEMENT, AssertionState.STRICTLY, ButtonState.DISABLED))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void verifyFieldState_READ_ONLY_treatsDisabledAsReadOnly() {
        // READ_ONLY is `isDisabled() || !isEditable()` -- a disabled field counts as
        // read-only even without checking isEditable() at all. Documented, not changed.
        when(locator.isDisabled()).thenReturn(true);

        assertThatNoException().isThrownBy(() ->
                flow.verifyFieldState(TestPage.HOME, ELEMENT, AssertionState.STRICTLY, FieldState.READ_ONLY));
    }

    @Test
    void verifyFieldState_EDITABLE_delegatesToLocatorIsEditable() {
        when(locator.isEditable()).thenReturn(true);

        assertThatNoException().isThrownBy(() ->
                flow.verifyFieldState(TestPage.HOME, ELEMENT, AssertionState.STRICTLY, FieldState.EDITABLE));
    }

    @Test
    void verifyToggleState_UNCHECKED_negatesIsChecked() {
        when(locator.isChecked()).thenReturn(false);

        assertThatNoException().isThrownBy(() ->
                flow.verifyToggleState(TestPage.HOME, ELEMENT, AssertionState.STRICTLY, ToggleState.UNCHECKED));
    }

    @Test
    void verifyTextEquals_normalizesCurlyApostrophe_beforeComparing() {
        when(elementActions.text(locator)).thenReturn("it’s fine");

        assertThatNoException().isThrownBy(() ->
                flow.verifyTextEquals(TestPage.HOME, ELEMENT, AssertionState.STRICTLY, "it's fine"));
    }

    @Test
    void verifyTextContains_delegatesToElementActionsText() {
        when(elementActions.text(locator)).thenReturn("Welcome back, User!");

        assertThatNoException().isThrownBy(() ->
                flow.verifyTextContains(TestPage.HOME, ELEMENT, AssertionState.STRICTLY, "back"));
    }

    @Test
    void verifyEquals_usesSuppliedContextDirectly() {
        assertThatNoException().isThrownBy(() ->
                flow.verifyEquals(AssertionState.STRICTLY, () -> 42, 42, "custom context"));
    }
}
