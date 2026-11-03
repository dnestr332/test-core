package com.dnestr.mobile.flows;

import com.dnestr.base.assertions.Softly;
import com.dnestr.base.states.AssertionState;
import com.dnestr.base.states.ButtonState;
import com.dnestr.base.states.FieldState;
import com.dnestr.base.states.VisibleState;
import com.dnestr.mobile.actions.ElementActions;
import com.dnestr.mobile.screens.ScreenElement;
import com.dnestr.mobile.context.TestContext;
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

/** {@link BaseActionFlow} and {@link BaseAssertionFlow} against an element with a fixed locator. */
@ExtendWith(MockitoExtension.class)
class BaseFlowsTest {

    enum SettingsElement implements ScreenElement {
        SAVE_BUTTON;

        @Override
        public By getLocator() {
            return LOCATOR;
        }
    }

    static class Actions extends BaseActionFlow {
        Actions(ElementActions elementActions) {
            super(elementActions);
        }
    }

    static class Assertions extends BaseAssertionFlow {
        Assertions(ElementActions elementActions) {
            super(elementActions);
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
    private WebElement element;

    private Assertions assertions;

    @BeforeEach
    void setUp() {
        assertions = new Assertions(elementActions);
    }

    @AfterEach
    void cleanup() {
        Softly.reset();
        System.clearProperty("platform");
        TestContext.clearPlatform();
    }

    @Test
    void actionFlowActsOnTheElementsLocator() {
        when(elementActions.text(LOCATOR)).thenReturn("Save");
        Actions actions = new Actions(elementActions);

        actions.tap(SettingsElement.SAVE_BUTTON);
        actions.type(SettingsElement.SAVE_BUTTON, "x");

        verify(elementActions).click(LOCATOR);
        verify(elementActions).type(LOCATOR, "x");
        assertThat(actions.text(SettingsElement.SAVE_BUTTON)).isEqualTo("Save");
    }

    @Test
    void verifyVisibleStrictlyFailsAtOnceAndNamesTheElement() {
        when(elementActions.isVisible(LOCATOR)).thenReturn(false);

        assertThatThrownBy(() -> assertions.verifyVisible(SettingsElement.SAVE_BUTTON, AssertionState.STRICTLY, VisibleState.VISIBLE))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("SettingsElement.SAVE_BUTTON should be visible");
    }

    @Test
    void verifyVisibleHiddenWaitsForTheElementToGo() {
        when(elementActions.isHidden(LOCATOR)).thenReturn(true);

        assertions.verifyVisible(SettingsElement.SAVE_BUTTON, AssertionState.SOFTLY, VisibleState.HIDDEN);

        assertThatNoException().isThrownBy(Softly::assertAll);
        verify(elementActions, never()).isVisible(LOCATOR);
        verify(elementActions, never()).isQuickVisible(LOCATOR);
    }

    @Test
    void verifyTextEqualsComparesTypographicApostrophesAsPlainOnes() {
        when(elementActions.text(LOCATOR)).thenReturn("Don’t save");

        assertThatNoException().isThrownBy(() ->
                assertions.verifyTextEquals(SettingsElement.SAVE_BUTTON, AssertionState.STRICTLY, "Don't save"));
    }

    @Test
    void verifyTextContainsWaitsForTextThatArrivesLate() {
        when(elementActions.text(LOCATOR)).thenReturn("Saving…", "Saving…", "Saved");

        assertions.verifyTextContains(SettingsElement.SAVE_BUTTON, "Saved");

        assertThatNoException().isThrownBy(Softly::assertAll);
    }

    @Test
    void verifyTextContainsUsesTheSubclassKeyword() {
        when(elementActions.text(LOCATOR)).thenReturn("anything");

        assertions.verifyTextContains(SettingsElement.SAVE_BUTTON, "not empty");

        assertThatNoException().isThrownBy(Softly::assertAll);
    }

    @Test
    void verifyButtonStateRecordsADisabledButtonSoftly() {
        when(elementActions.isEnabled(LOCATOR)).thenReturn(false);

        assertions.verifyButtonState(SettingsElement.SAVE_BUTTON, ButtonState.ENABLED);

        assertThatThrownBy(Softly::assertAll).hasMessageContaining("SAVE_BUTTON should be enabled");
    }

    @Test
    void verifyFieldStateReadsTheEnabledAttribute() {
        when(elementActions.find(LOCATOR)).thenReturn(element);
        when(element.getAttribute("enabled")).thenReturn("false");

        assertThatNoException().isThrownBy(() ->
                assertions.verifyFieldState(SettingsElement.SAVE_BUTTON, AssertionState.STRICTLY, FieldState.READ_ONLY));
        assertThatThrownBy(() ->
                assertions.verifyFieldState(SettingsElement.SAVE_BUTTON, AssertionState.STRICTLY, FieldState.EDITABLE))
                .hasMessageContaining("should be editable");
    }

    @Test
    void setCheckedTapsUntilTheBoxReadsAsWanted() {
        System.setProperty("platform", "android");
        when(elementActions.find(LOCATOR)).thenReturn(element);
        when(element.getAttribute("checked")).thenReturn("false", "true");

        new Actions(elementActions).setChecked(SettingsElement.SAVE_BUTTON, true);

        verify(elementActions, times(1)).click(LOCATOR);
    }

    @Test
    void setCheckedLeavesABoxThatAlreadyReadsAsWanted() {
        System.setProperty("platform", "android");
        when(elementActions.find(LOCATOR)).thenReturn(element);
        when(element.getAttribute("checked")).thenReturn("true");

        new Actions(elementActions).setChecked(SettingsElement.SAVE_BUTTON, true);

        verify(elementActions, never()).click(any(By.class));
    }

    @Test
    void softAssertTrueRecordsAReadThatThrowsAsAFailure() {
        assertions.softAssertTrue(() -> {
            throw new IllegalStateException("stale");
        }, "❌ read failed");

        assertThatThrownBy(Softly::assertAll).hasMessageContaining("read failed");
    }
}
