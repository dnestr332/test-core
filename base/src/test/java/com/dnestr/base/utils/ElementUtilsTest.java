package com.dnestr.base.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ElementUtilsTest {

    @Test
    void getStringAsDouble_parsesPlainInteger() {
        assertThat(ElementUtils.getStringAsDouble("42")).isEqualTo(42.0);
    }

    @Test
    void getStringAsDouble_parsesDecimal() {
        assertThat(ElementUtils.getStringAsDouble("$12.50")).isEqualTo(12.50);
    }

    @Test
    void getStringAsDouble_parsesNegativeValue() {
        assertThat(ElementUtils.getStringAsDouble("-5.3")).isEqualTo(-5.3);
        assertThat(ElementUtils.getStringAsDouble("Balance: -5.3 USD")).isEqualTo(-5.3);
    }

    @Test
    void getStringAsDouble_parsesNegativeInteger() {
        assertThat(ElementUtils.getStringAsDouble("-42")).isEqualTo(-42.0);
    }

    @Test
    void getStringAsDouble_noNumericValue_throws() {
        assertThatThrownBy(() -> ElementUtils.getStringAsDouble("no digits here"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no digits here");
    }
}
