package com.dnestr.base.logs;

import com.dnestr.base.actions.ElementAction;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/** The action line: marker, action, duration, locator and value, with a password field's value masked. */
class PrettyPrinterTest {

    static class FakePrettyPrinter extends PrettyPrinter<String> {
        @Override
        protected String describe(String locator) {
            return locator;
        }
    }

    private final FakePrettyPrinter printer = new FakePrettyPrinter();

    @Test
    void aClickReadsActionDurationAndLocator() {
        assertThat(printer.format(ElementAction.CLICK, "role=button[name=\"LOGIN\"i]", null, 0.1034))
                .isEqualTo("👉 click        0.10s  role=button[name=\"LOGIN\"i]");
    }

    @Test
    void aTypedValueIsQuoted() {
        assertThat(printer.format(ElementAction.TYPE, "role=textbox[name=\"Email\"i]", "qa@example.com", 0.7))
                .endsWith("role=textbox[name=\"Email\"i]  \"qa@example.com\"");
    }

    @Test
    void aValueTypedIntoAPasswordFieldIsMasked() {
        assertThat(printer.format(ElementAction.TYPE, "role=textbox[name=\"Password\"i]", "s3cret!", 0.2))
                .endsWith("\"********\"")
                .doesNotContain("s3cret!");
    }

    @Test
    void detailsOfOtherActionsAreNotQuoted() {
        assertThat(printer.format(ElementAction.LONG_PRESS, "id=card", "2000 ms", 2.0)).endsWith("id=card  2000 ms");
    }

    @Test
    void anActionWithoutALocatorPrintsOnlyItsNameAndDuration() {
        assertThat(printer.format(ElementAction.ZOOM, null, null, 0.181)).isEqualTo("👉 zoom         0.18s");
    }

    @Test
    void doneAndFailedLogWithoutThrowingEvenForAnErrorWithoutAMessage() {
        assertThatCode(() -> {
            printer.done(ElementAction.CLICK, "#ok", null, 0.1);
            printer.done(ElementAction.CLICK, "#slow", null, PrettyPrinter.SLOW_SECONDS + 1);
            printer.failed(ElementAction.CLICK, "#btn", null, 30, new RuntimeException("first line\nsecond line"));
            printer.failed(ElementAction.CLICK, "#btn", null, 30, new IllegalStateException());
        }).doesNotThrowAnyException();
    }
}
