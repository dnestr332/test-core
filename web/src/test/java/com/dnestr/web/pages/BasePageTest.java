package com.dnestr.web.pages;

import com.dnestr.base.states.VisibleState;
import com.dnestr.web.actions.ElementActions;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BasePageTest {

    @Mock
    private Page page;
    @Mock
    private ElementActions elementActions;
    @Mock
    private Locator resultLocator;
    @Mock
    private Locator withinLocator;
    @Mock
    private Locator textLocator;
    @Mock
    private Locator nthLocator;

    private BasePage basePage;

    @BeforeEach
    void setUp() {
        basePage = new BasePage(page, elementActions) {};
    }

    @Test
    void byRole_locatesByRoleAndSubstringName() {
        when(page.getByRole(eq(AriaRole.BUTTON), any(Page.GetByRoleOptions.class))).thenReturn(resultLocator);

        Locator result = basePage.byRole(AriaRole.BUTTON, "Submit");

        assertThat(result).isSameAs(resultLocator);
        ArgumentCaptor<Page.GetByRoleOptions> optionsCaptor = ArgumentCaptor.forClass(Page.GetByRoleOptions.class);
        verify(page).getByRole(eq(AriaRole.BUTTON), optionsCaptor.capture());
        assertThat(optionsCaptor.getValue().name).isEqualTo("Submit");
        assertThat(optionsCaptor.getValue().exact).isNull();
    }

    @Test
    void byRole_withExactFlag_setsExactOnOptions() {
        when(page.getByRole(eq(AriaRole.LINK), any(Page.GetByRoleOptions.class))).thenReturn(resultLocator);

        Locator result = basePage.byRole(AriaRole.LINK, "Home", true);

        assertThat(result).isSameAs(resultLocator);
        ArgumentCaptor<Page.GetByRoleOptions> optionsCaptor = ArgumentCaptor.forClass(Page.GetByRoleOptions.class);
        verify(page).getByRole(eq(AriaRole.LINK), optionsCaptor.capture());
        assertThat(optionsCaptor.getValue().name).isEqualTo("Home");
        assertThat(optionsCaptor.getValue().exact).isTrue();
    }

    @Test
    void byRole_withPattern_passesPatternAsName() {
        Pattern namePattern = Pattern.compile("Item \\d+");
        when(page.getByRole(eq(AriaRole.LISTITEM), any(Page.GetByRoleOptions.class))).thenReturn(resultLocator);

        Locator result = basePage.byRole(AriaRole.LISTITEM, namePattern);

        assertThat(result).isSameAs(resultLocator);
        ArgumentCaptor<Page.GetByRoleOptions> optionsCaptor = ArgumentCaptor.forClass(Page.GetByRoleOptions.class);
        verify(page).getByRole(eq(AriaRole.LISTITEM), optionsCaptor.capture());
        assertThat(optionsCaptor.getValue().name).isEqualTo(namePattern);
    }

    @Test
    void byRole_scopedToLocator_delegatesToWithinLocator() {
        when(withinLocator.getByRole(eq(AriaRole.BUTTON), any(Locator.GetByRoleOptions.class))).thenReturn(resultLocator);

        Locator result = basePage.byRole(withinLocator, AriaRole.BUTTON, "Delete");

        assertThat(result).isSameAs(resultLocator);
        ArgumentCaptor<Locator.GetByRoleOptions> optionsCaptor = ArgumentCaptor.forClass(Locator.GetByRoleOptions.class);
        verify(withinLocator).getByRole(eq(AriaRole.BUTTON), optionsCaptor.capture());
        assertThat(optionsCaptor.getValue().name).isEqualTo("Delete");
        verify(page, never()).getByRole(any(), any());
    }

    @Test
    void byRole_scopedToLocatorWithExactFlag_setsExactOnOptions() {
        when(withinLocator.getByRole(eq(AriaRole.BUTTON), any(Locator.GetByRoleOptions.class))).thenReturn(resultLocator);

        Locator result = basePage.byRole(withinLocator, AriaRole.BUTTON, "Delete", true);

        assertThat(result).isSameAs(resultLocator);
        ArgumentCaptor<Locator.GetByRoleOptions> optionsCaptor = ArgumentCaptor.forClass(Locator.GetByRoleOptions.class);
        verify(withinLocator).getByRole(eq(AriaRole.BUTTON), optionsCaptor.capture());
        assertThat(optionsCaptor.getValue().name).isEqualTo("Delete");
        assertThat(optionsCaptor.getValue().exact).isTrue();
    }

    @Test
    void byRole_scopedToLocatorWithPattern_passesPatternAsName() {
        Pattern namePattern = Pattern.compile("Row \\d+");
        when(withinLocator.getByRole(eq(AriaRole.ROW), any(Locator.GetByRoleOptions.class))).thenReturn(resultLocator);

        Locator result = basePage.byRole(withinLocator, AriaRole.ROW, namePattern);

        assertThat(result).isSameAs(resultLocator);
        ArgumentCaptor<Locator.GetByRoleOptions> optionsCaptor = ArgumentCaptor.forClass(Locator.GetByRoleOptions.class);
        verify(withinLocator).getByRole(eq(AriaRole.ROW), optionsCaptor.capture());
        assertThat(optionsCaptor.getValue().name).isEqualTo(namePattern);
    }

    @Test
    void byText_delegatesToPageGetByText() {
        when(page.getByText("Welcome")).thenReturn(resultLocator);

        Locator result = basePage.byText("Welcome");

        assertThat(result).isSameAs(resultLocator);
    }

    @Test
    void locator_resolvesPageElementViaByRole() {
        PageElement element = fakeElement(AriaRole.BUTTON, "Save");
        when(page.getByRole(eq(AriaRole.BUTTON), any(Page.GetByRoleOptions.class))).thenReturn(resultLocator);

        Locator result = basePage.locator(element);

        assertThat(result).isSameAs(resultLocator);
    }

    @Test
    void click_clicksLocatorResolvedFromPageElement() {
        PageElement element = fakeElement(AriaRole.BUTTON, "Save");
        when(page.getByRole(eq(AriaRole.BUTTON), any(Page.GetByRoleOptions.class))).thenReturn(resultLocator);

        basePage.click(element);

        verify(elementActions).click(resultLocator);
    }

    @Test
    void type_typesIntoLocatorResolvedFromPageElement() {
        PageElement element = fakeElement(AriaRole.TEXTBOX, "Email");
        when(page.getByRole(eq(AriaRole.TEXTBOX), any(Page.GetByRoleOptions.class))).thenReturn(resultLocator);

        basePage.type(element, "hello@example.com");

        verify(elementActions).type(resultLocator, "hello@example.com");
    }

    @Test
    void onlyMatch_returnsCandidates_whenConditionResolvesInTime() {
        when(elementActions.waitForCondition(any(), eq(1000))).thenReturn(true);

        Locator result = basePage.onlyMatch(resultLocator, "row", 1000);

        assertThat(result).isSameAs(resultLocator);
    }

    @Test
    void onlyMatch_throwsIllegalStateExceptionWithDescriptionAndCount_whenConditionTimesOut() {
        when(elementActions.waitForCondition(any(), eq(500))).thenReturn(false);
        when(resultLocator.count()).thenReturn(3);

        assertThatThrownBy(() -> basePage.onlyMatch(resultLocator, "row", 500))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Expected exactly one row, found 3");
    }

    @Test
    void isTextVisible_visible_returnsTrue_whenActualTextStartsWithExpectedText() {
        when(page.getByText("Welcome")).thenReturn(textLocator);
        when(textLocator.nth(0)).thenReturn(nthLocator);
        when(elementActions.text(nthLocator)).thenReturn("Welcome back!");

        boolean result = basePage.isTextVisible("Welcome", VisibleState.VISIBLE);

        assertThat(result).isTrue();
    }

    @Test
    void isTextVisible_visible_returnsFalse_whenActualTextDoesNotStartWithExpectedText() {
        when(page.getByText("Welcome")).thenReturn(textLocator);
        when(textLocator.nth(0)).thenReturn(nthLocator);
        when(elementActions.text(nthLocator)).thenReturn("Goodbye");

        boolean result = basePage.isTextVisible("Welcome", VisibleState.VISIBLE);

        assertThat(result).isFalse();
    }

    @Test
    void isTextVisible_hidden_delegatesToIsHiddenWithinTimeoutWithThreeSecondCap() {
        when(page.getByText("Loading")).thenReturn(textLocator);
        when(textLocator.nth(0)).thenReturn(nthLocator);
        when(elementActions.isHiddenWithinTimeout(nthLocator, 3000)).thenReturn(true);

        boolean result = basePage.isTextVisible("Loading", VisibleState.HIDDEN);

        assertThat(result).isTrue();
        verify(elementActions).isHiddenWithinTimeout(nthLocator, 3000);
    }

    @Test
    void isTextVisible_hidden_returnsFalse_whenTextStillVisibleAtTimeout() {
        when(page.getByText("Loading")).thenReturn(textLocator);
        when(textLocator.nth(0)).thenReturn(nthLocator);
        when(elementActions.isHiddenWithinTimeout(nthLocator, 3000)).thenReturn(false);

        boolean result = basePage.isTextVisible("Loading", VisibleState.HIDDEN);

        assertThat(result).isFalse();
    }

    private static PageElement fakeElement(AriaRole role, String label) {
        return new PageElement() {
            @Override
            public String getLabel() {
                return label;
            }

            @Override
            public AriaRole getRole() {
                return role;
            }
        };
    }
}
