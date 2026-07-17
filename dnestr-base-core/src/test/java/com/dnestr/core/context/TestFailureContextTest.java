package com.dnestr.core.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TestFailureContextTest {

    @AfterEach
    void cleanup() {
        TestFailureContext.clear();
    }

    @Test
    void setAndGetError_roundTrips() {
        RuntimeException error = new RuntimeException("boom");
        TestFailureContext.setError(error);
        assertThat(TestFailureContext.getError()).isSameAs(error);
    }

    @Test
    void clear_removesStoredError() {
        TestFailureContext.setError(new RuntimeException("boom"));
        TestFailureContext.clear();
        assertThat(TestFailureContext.getError()).isNull();
    }

    @Test
    void getError_whenNeverSet_returnsNull() {
        assertThat(TestFailureContext.getError()).isNull();
    }
}
