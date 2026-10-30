package com.dnestr.mobile.flows;

import com.dnestr.base.assertions.Softly;
import com.dnestr.base.states.AssertionState;
import com.dnestr.base.states.ButtonState;
import com.dnestr.base.states.FieldState;
import com.dnestr.base.states.VisibleState;
import com.dnestr.mobile.actions.ElementActions;
import com.dnestr.mobile.screens.BaseScreen;
import com.dnestr.mobile.screens.BaseScreenResolver;
import com.dnestr.mobile.screens.Clickable;
import com.dnestr.mobile.screens.AppScreen;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/** {@link BaseActionFlow} and {@link BaseAssertionFlow} against a screen whose one item resolves to a fixed locator. */
@ExtendWith(MockitoExtension.class)
class BaseFlowsTest {

    enum Item implements Clickable {
        SAVE_BUTTON;

        @Override
        public boolean isClickable() {
            return true;
        }
    }

    static class SettingsScreen extends BaseScreen {
        SettingsScreen(ElementActions elementActions) {
            super(elementActions);
        }

        @Override
        public By resolve(Clickable item) {
            return LOCATOR;
        }
    }

    enum State implements AppScreen {
        SETTINGS;

        @Override
        public Class<? extends BaseScreen> getScreenClass() {
            return SettingsScreen.class;
        }
    }

    static class Actions extends BaseActionFlow {
        Actions(ElementActions elementActions, BaseScreenResolver resolver) {
            super(elementActions, resolver);
        }
    }

    static class Assertions extends BaseAssertionFlow {
        Assertions(ElementActions elementActions, BaseScreenResolver resolver) {
            super(elementActions, resolver);
        }

        @Override
        protected boolean textMatches(String actual, String expected) {
            return "not empty".equals(expected) ? !actual.isBlank() : super.textMatches(actual, expected);
        }
    }

    private static final By LOCATOR = By.id("save");

    @Mock
    private ElementActions elementActions;
    @Mock
    private BaseScreenResolver resolver;
    @Mock
    private WebElement element;

    private SettingsScreen screen;
    private Assertions assertions;

    @BeforeEach
    void setUp() {
        screen = new SettingsScreen(elementActions);
        assertions = new Assertions(elementActions, resolver);
        lenient().when(resolver.resolveScreen(State.SETTINGS)).thenReturn(screen);
    }

    @AfterEach
    void cleanup() {
        Softly.reset();
    }

    @Test
    void actionFlowResolvesTheScreenThenActsOnItsItem() {
        when(elementActions.text(LOCATOR)).thenReturn("Save");
        Actions actions = new Actions(elementActions, resolver);

        actions.tap(State.SETTINGS, Item.SAVE_BUTTON);
        actions.type(State.SETTINGS, Item.SAVE_BUTTON, "x");

        verify(elementActions).click(LOCATOR);
        verify(elementActions).type(LOCATOR, "x");
        assertThat(actions.text(State.SETTINGS, Item.SAVE_BUTTON)).isEqualTo("Save");
    }

    @Test
    void verifyVisibleStrictlyFailsAtOnceAndNamesTheScreenAndItem() {
        when(elementActions.isVisible(LOCATOR)).thenReturn(false);

        assertThatThrownBy(() -> assertions.verifyVisible(State.SETTINGS, Item.SAVE_BUTTON, AssertionState.STRICTLY, VisibleState.VISIBLE))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("SettingsScreen | SAVE_BUTTON should be visible");
    }

    @Test
    void verifyVisibleHiddenWaitsForTheElementToGo() {
        when(elementActions.isHidden(LOCATOR)).thenReturn(true);

        assertions.verifyVisible(State.SETTINGS, Item.SAVE_BUTTON, AssertionState.SOFTLY, VisibleState.HIDDEN);

        assertThatNoException().isThrownBy(Softly::assertAll);
        verify(elementActions, never()).isVisible(LOCATOR);
        verify(elementActions, never()).isQuickVisible(LOCATOR);
    }

    @Test
    void verifyTextEqualsComparesTypographicApostrophesAsPlainOnes() {
        when(elementActions.text(LOCATOR)).thenReturn("Don’t save");

        assertThatNoException().isThrownBy(() ->
                assertions.verifyTextEquals(State.SETTINGS, Item.SAVE_BUTTON, AssertionState.STRICTLY, "Don't save"));
    }

    @Test
    void verifyTextContainsWaitsForTextThatArrivesLate() {
        when(elementActions.text(LOCATOR)).thenReturn("Saving…", "Saving…", "Saved");

        assertions.verifyTextContains(State.SETTINGS, Item.SAVE_BUTTON, "Saved");

        assertThatNoException().isThrownBy(Softly::assertAll);
    }

    @Test
    void verifyTextContainsUsesTheSubclassKeyword() {
        when(elementActions.text(LOCATOR)).thenReturn("anything");

        assertions.verifyTextContains(State.SETTINGS, Item.SAVE_BUTTON, "not empty");

        assertThatNoException().isThrownBy(Softly::assertAll);
    }

    @Test
    void verifyItemStateRecordsADisabledButtonSoftly() {
        when(elementActions.isEnabled(LOCATOR)).thenReturn(false);

        assertions.verifyItemState(State.SETTINGS, Item.SAVE_BUTTON, ButtonState.ENABLED);

        assertThatThrownBy(Softly::assertAll).hasMessageContaining("SAVE_BUTTON should be enabled");
    }

    @Test
    void verifyFieldStateReadsTheEnabledAttribute() {
        when(elementActions.find(LOCATOR)).thenReturn(element);
        when(element.getAttribute("enabled")).thenReturn("false");

        assertThatNoException().isThrownBy(() ->
                assertions.verifyFieldState(State.SETTINGS, Item.SAVE_BUTTON, AssertionState.STRICTLY, FieldState.READ_ONLY));
        assertThatThrownBy(() ->
                assertions.verifyFieldState(State.SETTINGS, Item.SAVE_BUTTON, AssertionState.STRICTLY, FieldState.EDITABLE))
                .hasMessageContaining("should be editable");
    }

    @Test
    void actionFlowRefusesAnItemMarkedNotClickableBeforeTouchingTheScreen() {
        Clickable label = () -> false;

        assertThatThrownBy(() -> new Actions(elementActions, resolver).tap(State.SETTINGS, label))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("is not clickable on screen SettingsScreen");
        verifyNoInteractions(elementActions);
    }

    @Test
    void softAssertTrueRecordsAReadThatThrowsAsAFailure() {
        assertions.softAssertTrue(() -> {
            throw new IllegalStateException("stale");
        }, "❌ read failed");

        assertThatThrownBy(Softly::assertAll).hasMessageContaining("read failed");
    }
}
