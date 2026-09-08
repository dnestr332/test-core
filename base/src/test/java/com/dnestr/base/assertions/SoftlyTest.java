package com.dnestr.base.assertions;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JUnit5 runs methods within a class sequentially on the same thread by default,
 * so these ordered tests exercise the same ThreadLocal a real thread-reusing
 * runner would leak across.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SoftlyTest {

    @AfterEach
    void cleanup() {
        Softly.reset();
    }

    @Test
    void isTrue_registersFailureWithoutThrowing() {
        Softly.isTrue(false, "should be true");
        assertThatThrownBy(Softly::assertAll).isInstanceOf(AssertionError.class);
    }

    @Test
    void isEqual_registersFailureDescribingActualAndExpected() {
        Softly.isEqual("actual", "expected", "values must match");
        assertThatThrownBy(Softly::assertAll)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("actual")
                .hasMessageContaining("expected");
    }

    @Test
    void isDoubleEqual_withinOffset_passes() {
        Softly.isDoubleEqual(1.001, 1.0, 0.01, "close enough");
        Softly.assertAll();
    }

    @Test
    @Order(1)
    void withoutReset_failureLeaksAcrossThreadReuse_beforeAssertAllIsReached() {
        Softly.isTrue(false, "boom");
        // Simulate the test throwing before Softly.assertAll() is ever reached,
        // bypassing @AfterEach's Softly.reset() call for THIS assertion only by
        // manually re-registering the leak the fixed code must survive.
        assertThat(Softly.getSoftly().errorsCollected()).hasSize(1);
    }

    @Test
    @Order(2)
    void reset_clearsStateSoNextTestOnSameThreadStartsFresh() {
        // Proves the fix: even though Order(1) left a failure registered and never
        // called assertAll(), @AfterEach's Softly.reset() cleared it, so this test
        // starts with a clean slate on the same (reused) thread.
        assertThat(Softly.getSoftly().errorsCollected()).isEmpty();
        Softly.assertAll();
    }
}
