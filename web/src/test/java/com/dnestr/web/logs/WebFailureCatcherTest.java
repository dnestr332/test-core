package com.dnestr.web.logs;

import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

/** A Playwright locator reads as its selector, without Playwright's own prefixes. */
class WebFailureCatcherTest {

    private final WebFailureCatcher catcher = new WebFailureCatcher();

    private Locator locatorToString(String value) {
        Locator locator = Mockito.mock(Locator.class);
        Mockito.when(locator.toString()).thenReturn(value);
        return locator;
    }

    @Test
    void aRoleLocatorLosesPlaywrightsPrefixes() {
        assertThat(catcher.describe(locatorToString("Locator@internal:role=button[name=\"LOGIN\"i]")))
                .isEqualTo("role=button[name=\"LOGIN\"i]");
    }

    @Test
    void aChainedLocatorKeepsEveryPart() {
        assertThat(catcher.describe(locatorToString("Locator@#rides >> internal:text=\"Save\"i >> nth=0")))
                .isEqualTo("#rides >> text=\"Save\"i >> nth=0");
    }

    @Test
    void aLocatorWithoutThePrefixIsPrintedAsIs() {
        assertThat(catcher.describe(locatorToString("locator('#submit')"))).isEqualTo("locator('#submit')");
    }
}
