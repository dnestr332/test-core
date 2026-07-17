package com.dnestr.web.logs;

import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Locks down {@link PrettyPrinter}'s substring/indexOf parsing of Playwright's
 * {@code Locator#toString()} and exception messages. This parsing depends on
 * undocumented Playwright internals and is brittle across version bumps -- these
 * tests exist to catch a silent format change, not to validate "correct" behavior.
 */
class PrettyPrinterTest {

    private final PrettyPrinter printer = new PrettyPrinter();

    private Locator locatorToString(String value) {
        Locator locator = Mockito.mock(Locator.class);
        Mockito.when(locator.toString()).thenReturn(value);
        return locator;
    }

    @Test
    void getLocatorType_splitsOnFirstColonSpace() {
        Locator locator = locatorToString("GetByRoleLocator: role=button, name='Submit'");

        assertThat(printer.getLocatorType(locator)).isEqualTo("GetByRoleLocator");
    }

    @Test
    void getLocatorValue_returnsRemainderAfterFirstColonSpace() {
        Locator locator = locatorToString("GetByRoleLocator: role=button, name='Submit'");

        assertThat(printer.getLocatorValue(locator)).isEqualTo("role=button, name='Submit'");
    }

    @Test
    void getLocatorType_withoutColonSpace_returnsWholeString() {
        Locator locator = locatorToString("locator('#submit')");

        assertThat(printer.getLocatorType(locator)).isEqualTo("locator('#submit')");
    }

    @Test
    void getLocatorValue_withoutColonSpace_returnsWholeStringToo() {
        // Current behavior: with no ": " separator, type and value both fall back
        // to the full toString() -- i.e. the same text is logged twice. Not fixed
        // here since it may be an accepted tradeoff for locators lacking a colon.
        Locator locator = locatorToString("locator('#submit')");

        assertThat(printer.getLocatorValue(locator)).isEqualTo("locator('#submit')");
    }

    @Test
    void extractLocatorFromMessage_findsLocatorCallSyntax() {
        String message = "Timeout 5000ms exceeded.\nlocator('button:has-text(\"Submit\")')\nwaiting for element";

        String extracted = printer.extractLocatorFromMessage(message);

        assertThat(extracted).startsWith("locator('button:has-text(\"Submit\")')");
    }

    @Test
    void extractLocatorFromMessage_fallsBackToGetBySyntax_whenNoLocatorCall() {
        String message = "Timeout exceeded waiting for getByRole('button', { name: 'Submit' })";

        String extracted = printer.extractLocatorFromMessage(message);

        assertThat(extracted).isEqualTo("getByRole('button', { name: 'Submit' })");
    }

    @Test
    void extractLocatorFromMessage_returnsNull_whenNeitherPatternPresent() {
        String message = "Some unrelated failure with no locator reference";

        assertThat(printer.extractLocatorFromMessage(message)).isNull();
    }
}
