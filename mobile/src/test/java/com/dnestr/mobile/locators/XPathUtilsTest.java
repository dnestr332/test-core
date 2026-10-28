package com.dnestr.mobile.locators;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class XPathUtilsTest {

    @Test
    void plainTextIsSingleQuoted() {
        assertThat(XPathUtils.literal("Call support")).isEqualTo("'Call support'");
    }

    @Test
    void textWithAnApostropheIsDoubleQuoted() {
        assertThat(XPathUtils.literal("Don't Allow")).isEqualTo("\"Don't Allow\"");
    }

    @Test
    void textWithBothQuotesIsJoinedWithConcat() {
        assertThat(XPathUtils.literal("it's \"done\"")).isEqualTo("concat('it', \"'\", 's \"done\"')");
    }

    @Test
    void aLeadingApostropheKeepsAnEmptyFirstPart() {
        assertThat(XPathUtils.literal("'a\"")).isEqualTo("concat('', \"'\", 'a\"')");
    }
}
