package com.dnestr.base.utils;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Runs real POSIX tools (sh, sleep), as the device tools it wraps are run. */
class ProcessUtilsTest {

    @Test
    void returnsTheOutputLinesWithStandardErrorMerged() {
        assertThat(ProcessUtils.run("sh", "-c", "echo one; echo two >&2"))
                .containsExactlyInAnyOrder("one", "two");
    }

    @Test
    void doesNotFailOnANonZeroExitCode() {
        assertThat(ProcessUtils.run("sh", "-c", "echo refused; exit 3")).containsExactly("refused");
    }

    @Test
    void killsACommandThatOutlivesTheTimeout() {
        long start = System.nanoTime();

        assertThatThrownBy(() -> ProcessUtils.run(Duration.ofMillis(300), "sleep", "5"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("`sleep 5` timed out after PT0.3S");
        assertThat(System.nanoTime() - start).isLessThan(3_000_000_000L);
    }

    @Test
    void namesAToolThatIsNotOnThePath() {
        assertThatThrownBy(() -> ProcessUtils.run("no-such-tool-xyz"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Is no-such-tool-xyz on PATH?");
    }
}
