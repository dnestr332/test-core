package com.dnestr.web.actions;

import com.dnestr.base.context.TestFailureContext;
import com.dnestr.web.logs.FailureCatcher;
import com.dnestr.web.logs.PrettyPrinter;
import com.microsoft.playwright.Keyboard;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Page.WaitForConditionOptions;
import com.microsoft.playwright.Page.WaitForFunctionOptions;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ElementActionsTest {

    @Mock
    private Page page;
    @Mock
    private Locator locator;
    @Mock
    private Keyboard keyboard;

    private ElementActions actions;

    @BeforeEach
    void setUp() {
        actions = new ElementActions(page, new FailureCatcher(new PrettyPrinter()));
    }

    @AfterEach
    void tearDown() {
        TestFailureContext.clear();
    }

    @Test
    void click_delegatesToLocatorClick() {
        actions.click(locator);

        verify(locator).click();
    }

    @Test
    void type_delegatesToLocatorFill() {
        actions.type(locator, "hello");

        verify(locator).fill("hello");
    }

    @Test
    void typeWithKeyboard_clicksClearsThenTypesViaKeyboard_inOrder() {
        when(page.keyboard()).thenReturn(keyboard);

        actions.typeWithKeyboard(locator, "hello");

        InOrder inOrder = inOrder(locator, keyboard);
        inOrder.verify(locator).click();
        inOrder.verify(locator).clear();
        inOrder.verify(keyboard).type("hello");
    }

    @Test
    void clearAndType_clicksSelectsAllDeletesThenFills_inOrder() {
        actions.clearAndType(locator, "hello");

        InOrder inOrder = inOrder(locator);
        inOrder.verify(locator).click();
        inOrder.verify(locator).press("Control+A");
        inOrder.verify(locator).press("Backspace");
        inOrder.verify(locator).fill("hello");
    }

    @Test
    void press_delegatesToLocatorPress() {
        actions.press(locator, "Enter");

        verify(locator).press("Enter");
    }

    @Test
    void text_returnsTrimmedInnerText_whenPresent() {
        when(locator.innerText()).thenReturn("  hello  ");

        String result = actions.text(locator);

        assertThat(result).isEqualTo("hello");
        verify(locator, never()).inputValue();
    }

    @Test
    void text_fallsBackToInputValue_whenInnerTextBlank() {
        when(locator.innerText()).thenReturn("   ");
        when(locator.inputValue()).thenReturn(" value ");

        String result = actions.text(locator);

        assertThat(result).isEqualTo("value");
    }

    @Test
    void text_returnsEmptyString_whenBothInnerTextAndInputValueBlank() {
        when(locator.innerText()).thenReturn("");
        when(locator.inputValue()).thenReturn(null);

        String result = actions.text(locator);

        assertThat(result).isEmpty();
    }

    @Test
    void getTexts_trimsEachEntry() {
        when(locator.allTextContents()).thenReturn(List.of(" a ", "b", " c"));

        List<String> result = actions.getTexts(locator);

        assertThat(result).containsExactly("a", "b", "c");
    }

    @Test
    void getAttributes_iteratesByIndexAndTrimsNonNullValues() {
        Locator nth0 = mock(Locator.class);
        Locator nth1 = mock(Locator.class);
        when(locator.count()).thenReturn(2);
        when(locator.nth(0)).thenReturn(nth0);
        when(locator.nth(1)).thenReturn(nth1);
        when(nth0.getAttribute("data-x")).thenReturn(" foo ");
        when(nth1.getAttribute("data-x")).thenReturn(null);

        List<String> result = actions.getAttributes(locator, "data-x");

        assertThat(result).containsExactly("foo", null);
    }

    @Test
    void scrollIntoView_delegatesToLocator() {
        actions.scrollIntoView(locator);

        verify(locator).scrollIntoViewIfNeeded();
    }

    @Test
    void scrollToTop_evaluatesScrollToOrigin() {
        actions.scrollToTop();

        verify(page).evaluate("window.scrollTo(0, 0)");
    }

    @Test
    void scrollToBottom_evaluatesScrollToBodyHeight() {
        actions.scrollToBottom();

        verify(page).evaluate("window.scrollTo(0, document.body.scrollHeight)");
    }

    @Test
    void scrollBy_passesXAndYAsArgsMap() {
        actions.scrollBy(10, 20);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Integer>> argsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(page).evaluate(eq("args => window.scrollBy(args.x, args.y)"), argsCaptor.capture());
        assertThat(argsCaptor.getValue()).containsEntry("x", 10).containsEntry("y", 20);
    }

    @Test
    void hover_delegatesToLocator() {
        actions.hover(locator);

        verify(locator).hover();
    }

    @Test
    void setZoom_evaluatesScriptWithScaleArgument() {
        actions.setZoom(1.5);

        ArgumentCaptor<Double> scaleCaptor = ArgumentCaptor.forClass(Double.class);
        verify(page).evaluate(contains("transform"), scaleCaptor.capture());
        assertThat(scaleCaptor.getValue()).isEqualTo(1.5);
    }

    @Test
    void setCssZoom_evaluatesScriptWithScaleArgument() {
        actions.setCssZoom(1.5);

        ArgumentCaptor<Double> scaleCaptor = ArgumentCaptor.forClass(Double.class);
        verify(page).evaluate(contains("zoom"), scaleCaptor.capture());
        assertThat(scaleCaptor.getValue()).isEqualTo(1.5);
    }

    @Test
    void waitForCondition_returnsTrue_whenPageConditionResolves() {
        BooleanSupplier condition = () -> true;

        boolean result = actions.waitForCondition(condition, 1000);

        assertThat(result).isTrue();
        ArgumentCaptor<WaitForConditionOptions> optionsCaptor = ArgumentCaptor.forClass(WaitForConditionOptions.class);
        verify(page).waitForCondition(eq(condition), optionsCaptor.capture());
        assertThat(optionsCaptor.getValue().timeout).isEqualTo(1000.0);
    }

    @Test
    void waitForCondition_returnsFalse_onTimeout_insteadOfThrowing() {
        doThrow(new TimeoutError("timed out")).when(page).waitForCondition(any(), any());

        boolean result = actions.waitForCondition(() -> false, 500);

        assertThat(result).isFalse();
    }

    @Test
    void isVisibleWithinTimeout_returnsTrue_andWaitsForVisibleState() {
        boolean result = actions.isVisibleWithinTimeout(locator, 1000);

        assertThat(result).isTrue();
        ArgumentCaptor<Locator.WaitForOptions> optionsCaptor = ArgumentCaptor.forClass(Locator.WaitForOptions.class);
        verify(locator).waitFor(optionsCaptor.capture());
        assertThat(optionsCaptor.getValue().state).isEqualTo(WaitForSelectorState.VISIBLE);
        assertThat(optionsCaptor.getValue().timeout).isEqualTo(1000.0);
    }

    @Test
    void isVisibleWithinTimeout_returnsFalse_onTimeout_insteadOfThrowing() {
        doThrow(new TimeoutError("timed out")).when(locator).waitFor(any());

        boolean result = actions.isVisibleWithinTimeout(locator, 500);

        assertThat(result).isFalse();
    }

    @Test
    void isHiddenWithinTimeout_waitsForHiddenState() {
        boolean result = actions.isHiddenWithinTimeout(locator, 1000);

        assertThat(result).isTrue();
        ArgumentCaptor<Locator.WaitForOptions> optionsCaptor = ArgumentCaptor.forClass(Locator.WaitForOptions.class);
        verify(locator).waitFor(optionsCaptor.capture());
        assertThat(optionsCaptor.getValue().state).isEqualTo(WaitForSelectorState.HIDDEN);
    }

    @Test
    void clickIfVisible_clicksAndReturnsTrue_whenLocatorAppearsInTime() {
        boolean result = actions.clickIfVisible(locator, 1000);

        assertThat(result).isTrue();
        verify(locator).click();
    }

    @Test
    void clickIfVisible_returnsFalseWithoutClicking_whenLocatorNeverAppears() {
        doThrow(new TimeoutError("timed out")).when(locator).waitFor(any());

        boolean result = actions.clickIfVisible(locator, 500);

        assertThat(result).isFalse();
        verify(locator, never()).click();
    }

    @Test
    void waitForStability_noArgs_usesDefaultDebounceAndMaxWait() {
        actions.waitForStability();

        ArgumentCaptor<String> scriptCaptor = ArgumentCaptor.forClass(String.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Integer>> argCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<WaitForFunctionOptions> optionsCaptor = ArgumentCaptor.forClass(WaitForFunctionOptions.class);
        verify(page).waitForFunction(scriptCaptor.capture(), argCaptor.capture(), optionsCaptor.capture());

        assertThat(argCaptor.getValue()).containsEntry("debounceMs", 400).containsEntry("maxWaitMs", 5000);
        assertThat(optionsCaptor.getValue().timeout).isEqualTo(10000.0);
    }

    @Test
    void waitForStability_customDebounceAndMaxWait_passesThemAsScriptArgumentsAndDerivesTimeout() {
        actions.waitForStability(100, 2000);

        ArgumentCaptor<String> scriptCaptor = ArgumentCaptor.forClass(String.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Integer>> argCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<WaitForFunctionOptions> optionsCaptor = ArgumentCaptor.forClass(WaitForFunctionOptions.class);
        verify(page).waitForFunction(scriptCaptor.capture(), argCaptor.capture(), optionsCaptor.capture());

        assertThat(argCaptor.getValue()).containsEntry("debounceMs", 100).containsEntry("maxWaitMs", 2000);
        // Playwright-side timeout must exceed the script's own max-wait cap, or Playwright
        // would throw a TimeoutError before the script gets a chance to resolve(true) itself.
        assertThat(optionsCaptor.getValue().timeout).isEqualTo(7000.0);
    }

    @Test
    void waitForStability_scriptObservesCharacterDataAndAlwaysDisconnectsViaAbsoluteCap() {
        actions.waitForStability();

        ArgumentCaptor<String> scriptCaptor = ArgumentCaptor.forClass(String.class);
        verify(page).waitForFunction(scriptCaptor.capture(), any(), any(WaitForFunctionOptions.class));

        String script = scriptCaptor.getValue();
        assertThat(script).contains("MutationObserver");
        // Plain text updates (no attribute/childList change) must still reset the debounce.
        assertThat(script).contains("characterData: true");
        // An absolute cap independent of the debounce timer guarantees the observer is
        // disconnected and the promise resolves even under continuous DOM mutation.
        assertThat(script).contains("maxWaitTimer");
        assertThat(script).contains("observer.disconnect()");
    }

    @Test
    void click_whenLocatorThrows_recordsErrorInTestFailureContextAndRethrows() {
        RuntimeException boom = new RuntimeException("boom");
        doThrow(boom).when(locator).click();

        assertThatThrownBy(() -> actions.click(locator)).isSameAs(boom);
        assertThat(TestFailureContext.getError()).isSameAs(boom);
    }
}
