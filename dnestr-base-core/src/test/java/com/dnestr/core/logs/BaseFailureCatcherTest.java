package com.dnestr.core.logs;

import com.dnestr.core.actions.ElementAction;
import com.dnestr.core.context.TestFailureContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

class BaseFailureCatcherTest {

    static class FakeCatcher extends BaseFailureCatcher<String> {
        FakeCatcher(BasePrettyPrinter<String> printer) {
            super(printer);
        }
    }

    @SuppressWarnings("unchecked")
    private final BasePrettyPrinter<String> printer = Mockito.mock(BasePrettyPrinter.class);
    private final FakeCatcher catcher = new FakeCatcher(printer);

    @AfterEach
    void cleanup() {
        TestFailureContext.clear();
    }

    @Test
    void withFailureCapture_supplier_returnsValueAndLogsOk() {
        String result = catcher.withFailureCapture(ElementAction.GET_TEXT, "#field", null, () -> "value");

        assertThat(result).isEqualTo("value");
        verify(printer).start(ElementAction.GET_TEXT, "#field", null);
        verify(printer).ok(Mockito.anyDouble());
    }

    @Test
    void withFailureCapture_supplierThrows_logsFailAndRecordsInFailureContext() {
        RuntimeException boom = new RuntimeException("boom");

        assertThatThrownBy(() -> catcher.withFailureCapture(ElementAction.CLICK, "#btn", "details",
                () -> { throw boom; }))
                .isSameAs(boom);

        verify(printer).fail(boom);
        assertThat(TestFailureContext.getError()).isSameAs(boom);
    }

    @Test
    void withFailureCapture_runnable_delegatesToSupplierVariant() {
        boolean[] executed = { false };

        catcher.withFailureCapture(ElementAction.CLICK, "#btn", null, () -> executed[0] = true);

        assertThat(executed[0]).isTrue();
        verify(printer).ok(Mockito.anyDouble());
    }
}
