package com.dnestr.web.assertions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Only isTrue/isFalse/isEqual are covered: they're plain AssertJ wrappers with
 * real branching logic. The remaining Hardly methods (isVisible, isHidden,
 * isEnabled, isDisabled, isChecked, hasText, containsText, hasAttribute) are
 * one-line delegations straight into Playwright's PlaywrightAssertions/
 * LocatorAssertions, which poll against a live browser connection -- a Mockito
 * mock of Locator cannot stand in for that, and there's no independent logic
 * in this class left to verify once the delegation itself is excluded.
 */
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
}
