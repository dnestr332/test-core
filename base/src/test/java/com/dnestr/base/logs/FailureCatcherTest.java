package com.dnestr.base.logs;

import com.dnestr.base.actions.ElementAction;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * The catcher logs an action once it ends, with its duration, and rethrows a failure unchanged; the action line reads
 * marker, action, duration, locator and value, with a password field's value masked.
 */
class FailureCatcherTest {

    static class FakeCatcher extends FailureCatcher<String> {
        @Override
        protected String describe(String locator) {
            return locator;
        }
    }

    private final FakeCatcher catcher = Mockito.spy(new FakeCatcher());

    @Test
    void aSucceedingActionReturnsItsValueAndLogsOnceAfterward() {
        String result = catcher.withFailureCapture(ElementAction.GET_TEXT, "#field", null, () -> "value");

        assertThat(result).isEqualTo("value");
        verify(catcher).done(eq(ElementAction.GET_TEXT), eq("#field"), isNull(), anyDouble());
        verify(catcher, never()).failed(Mockito.any(), Mockito.any(), Mockito.any(), anyDouble(), Mockito.any());
    }

    @Test
    void aFailingActionLogsTheErrorAndRethrowsItUnchanged() {
        RuntimeException boom = new RuntimeException("boom");

        assertThatThrownBy(() -> catcher.withFailureCapture(ElementAction.CLICK, "#btn", "details",
                () -> { throw boom; }))
                .isSameAs(boom);

        verify(catcher).failed(eq(ElementAction.CLICK), eq("#btn"), eq("details"), anyDouble(), same(boom));
        verify(catcher, never()).done(Mockito.any(), Mockito.any(), Mockito.any(), anyDouble());
    }

    @Test
    void theRunnableOverloadRunsTheActionAndLogsIt() {
        boolean[] executed = { false };

        catcher.withFailureCapture(ElementAction.CLICK, "#btn", null, () -> executed[0] = true);

        assertThat(executed[0]).isTrue();
        verify(catcher).done(eq(ElementAction.CLICK), eq("#btn"), isNull(), anyDouble());
    }

    @Test
    void aClickReadsActionDurationAndLocator() {
        assertThat(catcher.format(ElementAction.CLICK, "role=button[name=\"LOGIN\"i]", null, 0.1034))
                .isEqualTo("👉 CLICK        0.10s  role=button[name=\"LOGIN\"i]");
    }

    @Test
    void aTypedValueIsQuoted() {
        assertThat(catcher.format(ElementAction.TYPE, "role=textbox[name=\"Email\"i]", "qa@example.com", 0.7))
                .endsWith("role=textbox[name=\"Email\"i]  \"qa@example.com\"");
    }

    @Test
    void aValueTypedIntoAPasswordFieldIsMasked() {
        assertThat(catcher.format(ElementAction.TYPE, "role=textbox[name=\"Password\"i]", "s3cret!", 0.2))
                .endsWith("\"********\"")
                .doesNotContain("s3cret!");
    }

    @Test
    void detailsOfOtherActionsAreNotQuoted() {
        assertThat(catcher.format(ElementAction.LONG_PRESS, "id=card", "2000 ms", 2.0)).endsWith("id=card  2000 ms");
    }

    @Test
    void anActionWithoutALocatorPrintsOnlyItsNameAndDuration() {
        assertThat(catcher.format(ElementAction.ZOOM, null, null, 0.181)).isEqualTo("👉 ZOOM         0.18s");
    }

    @Test
    void doneAndFailedLogWithoutThrowingEvenForAnErrorWithoutAMessage() {
        assertThatCode(() -> {
            catcher.done(ElementAction.CLICK, "#ok", null, 0.1);
            catcher.done(ElementAction.CLICK, "#slow", null, FailureCatcher.SLOW_SECONDS + 1);
            catcher.failed(ElementAction.CLICK, "#btn", null, 30, new RuntimeException("first line\nsecond line"));
            catcher.failed(ElementAction.CLICK, "#btn", null, 30, new IllegalStateException());
        }).doesNotThrowAnyException();
    }
}
