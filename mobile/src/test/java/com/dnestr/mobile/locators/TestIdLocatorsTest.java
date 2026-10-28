package com.dnestr.mobile.locators;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static org.assertj.core.api.Assertions.assertThat;

class TestIdLocatorsTest {

    @Test
    void byTestIdMatchesResourceIdOrName() {
        assertThat(TestIdLocators.byTestId("online-toggle"))
                .isEqualTo(By.xpath("//*[@resource-id='online-toggle' or @name='online-toggle']"));
    }

    @Test
    void byAnyTestIdMatchesEitherId() {
        assertThat(TestIdLocators.byAnyTestId("online-toggle", "otp-error"))
                .isEqualTo(By.xpath("//*[@resource-id='online-toggle' or @name='online-toggle'"
                        + " or @resource-id='otp-error' or @name='otp-error']"));
    }

    @Test
    void prefixLocatorsMatchTheStartOfTheId() {
        assertThat(TestIdLocators.firstByTestIdPrefix("next-ride-"))
                .isEqualTo(By.xpath("(//*[starts-with(@resource-id,'next-ride-') or starts-with(@name,'next-ride-')])[1]"));
        assertThat(TestIdLocators.allByTestIdPrefix("next-ride-"))
                .isEqualTo(By.xpath("//*[starts-with(@resource-id,'next-ride-') or starts-with(@name,'next-ride-')]"));
    }

    @Test
    void anIdWithAQuoteStillMakesValidXPath() {
        assertThat(TestIdLocators.byTestId("o'clock"))
                .isEqualTo(By.xpath("//*[@resource-id=\"o'clock\" or @name=\"o'clock\"]"));
    }
}
