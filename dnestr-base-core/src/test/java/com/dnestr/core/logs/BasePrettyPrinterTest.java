package com.dnestr.core.logs;

import com.dnestr.core.actions.ElementAction;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class BasePrettyPrinterTest {

    static class FakePrettyPrinter extends BasePrettyPrinter<String> {
        @Override
        protected String getLocatorType(String locator) {
            return "css";
        }

        @Override
        protected String getLocatorValue(String locator) {
            return locator;
        }
    }

    private final FakePrettyPrinter printer = new FakePrettyPrinter();

    @Test
    void start_withLocator_doesNotThrow() {
        assertThatCode(() -> printer.start(ElementAction.CLICK, "#submit", "extra details"))
                .doesNotThrowAnyException();
    }

    @Test
    void start_withoutLocator_doesNotThrow() {
        assertThatCode(() -> printer.start(ElementAction.CLICK, null, null))
                .doesNotThrowAnyException();
    }

    @Test
    void ok_doesNotThrow() {
        assertThatCode(() -> printer.ok(0.42)).doesNotThrowAnyException();
    }

    @Test
    void fail_withMultilineMessage_usesOnlyFirstLine() {
        Throwable error = new RuntimeException("first line\nsecond line");
        assertThatCode(() -> printer.fail(error)).doesNotThrowAnyException();
    }

    @Test
    void fail_withNullMessage_doesNotThrow() {
        Throwable error = new RuntimeException();
        assertThatCode(() -> printer.fail(error)).doesNotThrowAnyException();
    }

    @Test
    void fail_withNoStackTrace_doesNotThrow() {
        Throwable error = new RuntimeException("boom");
        error.setStackTrace(new StackTraceElement[0]);
        assertThatCode(() -> printer.fail(error)).doesNotThrowAnyException();
    }
}
