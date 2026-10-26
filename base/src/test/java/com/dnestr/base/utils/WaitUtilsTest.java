package com.dnestr.base.utils;

import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WaitUtilsTest {

    @Test
    void waitUntil_returnsOnceConditionBecomesTrue() {
        AtomicInteger polls = new AtomicInteger();

        WaitUtils.waitUntil(() -> polls.incrementAndGet() >= 3, 2, 10, "three polls");

        assertThat(polls).hasValue(3);
    }

    @Test
    void waitUntil_timesOut_whenConditionNeverTrue() {
        assertThatThrownBy(() -> WaitUtils.waitUntil(() -> false, 1, 50, "never"))
                .isInstanceOf(ConditionTimeoutException.class);
    }

    @Test
    void waitUntil_propagatesExceptionFromCondition() {
        assertThatThrownBy(() -> WaitUtils.waitUntil(() -> {
            throw new IllegalStateException("boom");
        }, 1, 50, "throws"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");
    }

    @Test
    void waitUntilIgnoringExceptions_retriesPastTransientExceptions() {
        AtomicInteger polls = new AtomicInteger();

        WaitUtils.waitUntilIgnoringExceptions(() -> {
            if (polls.incrementAndGet() < 3) {
                throw new IllegalStateException("not ready");
            }
            return true;
        }, 2, "settles");

        assertThat(polls).hasValue(3);
    }

    @Test
    void waitUntilIgnoringExceptions_timesOut_whenConditionKeepsThrowing() {
        assertThatThrownBy(() -> WaitUtils.waitUntilIgnoringExceptions(() -> {
            throw new IllegalStateException("never ready");
        }, 1, "always throws"))
                .isInstanceOf(ConditionTimeoutException.class);
    }

    @Test
    void waitForValue_returnsFirstValueMatchingCondition() {
        AtomicInteger counter = new AtomicInteger();

        int value = WaitUtils.waitForValue(counter::incrementAndGet, v -> v >= 3, 2, "reach 3");

        assertThat(value).isEqualTo(3);
    }

    @Test
    void waitForValue_timesOut_whenNoValueMatches() {
        assertThatThrownBy(() -> WaitUtils.waitForValue(() -> "x", "y"::equals, 1, "never y"))
                .isInstanceOf(ConditionTimeoutException.class);
    }

    @Test
    void sleepMillis_whenInterrupted_restoresInterruptFlagInsteadOfThrowing() {
        Thread.currentThread().interrupt();
        try {
            WaitUtils.sleepMillis(1_000);

            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }
}
