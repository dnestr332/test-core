package com.dnestr.mobile.logs;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Locks down {@link MobilePrettyPrinter}'s parsing of Selenium's {@code By#toString()}
 * ({@code "By.<strategy>: <value>"}) and of exception messages that embed one.
 */
class MobilePrettyPrinterTest {

    private final MobilePrettyPrinter printer = new MobilePrettyPrinter();

    @Test
    void getLocatorType_returnsStrategyBeforeFirstColonSpace() {
        assertThat(printer.getLocatorType(By.id("submit-button"))).isEqualTo("By.id");
    }

    @Test
    void getLocatorValue_returnsRemainderAfterFirstColonSpace() {
        assertThat(printer.getLocatorValue(By.xpath("//a[@title='x: y']"))).isEqualTo("//a[@title='x: y']");
    }

    @Test
    void typeAndValue_withoutColonSpace_bothReturnWholeString() {
        By locator = new By() {
            @Override
            public List<WebElement> findElements(SearchContext context) {
                return List.of();
            }

            @Override
            public String toString() {
                return "custom";
            }
        };

        assertThat(printer.getLocatorType(locator)).isEqualTo("custom");
        assertThat(printer.getLocatorValue(locator)).isEqualTo("custom");
    }

    @Test
    void extractLocatorFromMessage_returnsFromByToEnd() {
        String message = "no such element: Unable to locate element: By.id: submit-button  ";

        assertThat(printer.extractLocatorFromMessage(message)).isEqualTo("By.id: submit-button");
    }

    @Test
    void extractLocatorFromMessage_returnsNull_whenNoByPresent() {
        assertThat(printer.extractLocatorFromMessage("Session not created")).isNull();
    }
}
