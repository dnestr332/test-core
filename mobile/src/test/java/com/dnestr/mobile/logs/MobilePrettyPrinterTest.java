package com.dnestr.mobile.logs;

import org.junit.jupiter.api.Test;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** A {@code By} reads as {@code strategy=value} in an action line. */
class MobilePrettyPrinterTest {

    private final MobilePrettyPrinter printer = new MobilePrettyPrinter();

    @Test
    void aSeleniumLocatorReadsAsStrategyEqualsValue() {
        assertThat(printer.describe(By.id("submit-button"))).isEqualTo("id=submit-button");
        assertThat(printer.describe(By.xpath("//a[@title='x: y']"))).isEqualTo("xpath=//a[@title='x: y']");
    }

    @Test
    void anAppiumLocatorReadsTheSameWay() {
        assertThat(printer.describe(AppiumBy.accessibilityId("Menu"))).isEqualTo("accessibilityId=Menu");
    }

    @Test
    void aLocatorWithAnUnknownFormatIsPrintedAsIs() {
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

        assertThat(printer.describe(locator)).isEqualTo("custom");
    }
}
