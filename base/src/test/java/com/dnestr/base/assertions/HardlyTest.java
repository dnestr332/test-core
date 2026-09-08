package com.dnestr.base.assertions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HardlyTest {

    @Test
    void isTrue_passesSilently_whenConditionTrue() {
        assertThatNoException().isThrownBy(() -> Hardly.isTrue(true, "context"));
    }

    @Test
    void isTrue_throwsAssertionError_whenConditionFalse() {
        assertThatThrownBy(() -> Hardly.isTrue(false, "context"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("context");
    }

    @Test
    void isFalse_passesSilently_whenConditionFalse() {
        assertThatNoException().isThrownBy(() -> Hardly.isFalse(false, "context"));
    }

    @Test
    void isFalse_throwsAssertionError_whenConditionTrue() {
        assertThatThrownBy(() -> Hardly.isFalse(true, "context"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("context");
    }

    @Test
    void isEqual_passesSilently_whenEqual() {
        assertThatNoException().isThrownBy(() -> Hardly.isEqual("a", "a", "context"));
    }

    @Test
    void isEqual_throwsAssertionError_whenNotEqual() {
        assertThatThrownBy(() -> Hardly.isEqual("a", "b", "context"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("context");
    }

    @Test
    void fail_alwaysThrowsAssertionError_withGivenMessage() {
        assertThatThrownBy(() -> Hardly.fail("boom"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("boom");
    }
}
