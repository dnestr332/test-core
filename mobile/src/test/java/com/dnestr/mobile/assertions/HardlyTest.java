package com.dnestr.mobile.assertions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HardlyTest {

    @Test
    void isTruePassesForTrueCondition() {
        assertDoesNotThrow(() -> Hardly.isTrue(true, "context"));
    }

    @Test
    void isTrueFailsForFalseCondition() {
        assertThrows(AssertionError.class, () -> Hardly.isTrue(false, "context"));
    }

    @Test
    void isFalsePassesForFalseCondition() {
        assertDoesNotThrow(() -> Hardly.isFalse(false, "context"));
    }

    @Test
    void isFalseFailsForTrueCondition() {
        assertThrows(AssertionError.class, () -> Hardly.isFalse(true, "context"));
    }

    @Test
    void isEqualPassesForEqualValues() {
        assertDoesNotThrow(() -> Hardly.isEqual("a", "a", "context"));
    }

    @Test
    void isEqualFailsForDifferentValues() {
        AssertionError error = assertThrows(AssertionError.class,
                () -> Hardly.isEqual("a", "b", "context"));
        assertThat(error.getMessage()).contains("context");
    }

    @Test
    void failThrowsAssertionErrorWithMessage() {
        AssertionError error = assertThrows(AssertionError.class, () -> Hardly.fail("boom"));
        assertThat(error.getMessage()).contains("boom");
    }
}
