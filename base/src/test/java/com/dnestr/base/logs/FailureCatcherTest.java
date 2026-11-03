package com.dnestr.base.logs;

import com.dnestr.base.actions.ElementAction;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/** The catcher logs an action once it ends, with its duration, and rethrows a failure unchanged. */
class FailureCatcherTest {

    static class FakeCatcher extends FailureCatcher<String> {
        FakeCatcher(PrettyPrinter<String> printer) {
            super(printer);
        }
    }

    @SuppressWarnings("unchecked")
    private final PrettyPrinter<String> printer = Mockito.mock(PrettyPrinter.class);
    private final FakeCatcher catcher = new FakeCatcher(printer);

    @Test
    void aSucceedingActionReturnsItsValueAndLogsOnceAfterward() {
        String result = catcher.withFailureCapture(ElementAction.GET_TEXT, "#field", null, () -> "value");

        assertThat(result).isEqualTo("value");
        verify(printer).done(eq(ElementAction.GET_TEXT), eq("#field"), isNull(), anyDouble());
        verifyNoMoreInteractions(printer);
    }

    @Test
    void aFailingActionLogsTheErrorAndRethrowsItUnchanged() {
        RuntimeException boom = new RuntimeException("boom");

        assertThatThrownBy(() -> catcher.withFailureCapture(ElementAction.CLICK, "#btn", "details",
                () -> { throw boom; }))
                .isSameAs(boom);

        verify(printer).failed(eq(ElementAction.CLICK), eq("#btn"), eq("details"), anyDouble(), same(boom));
        verifyNoMoreInteractions(printer);
    }

    @Test
    void theRunnableOverloadRunsTheActionAndLogsIt() {
        boolean[] executed = { false };

        catcher.withFailureCapture(ElementAction.CLICK, "#btn", null, () -> executed[0] = true);

        assertThat(executed[0]).isTrue();
        verify(printer).done(eq(ElementAction.CLICK), eq("#btn"), isNull(), anyDouble());
    }
}
