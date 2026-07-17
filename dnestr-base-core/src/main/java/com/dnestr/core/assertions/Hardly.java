package com.dnestr.core.assertions;

import org.assertj.core.api.Assertions;

public final class Hardly {

    private Hardly() {}

    public static void isTrue(boolean condition, String context) {
        Assertions.assertThat(condition)
                .as("%s | Condition: <%s> should be true: ", context, condition)
                .isTrue();
    }

    public static void isFalse(boolean condition, String context) {
        Assertions.assertThat(condition)
                .as("%s | Condition: <%s> should be false", context, condition)
                .isFalse();
    }

    public static <T> void isEqual(T actual, T expected, String context) {
        Assertions.assertThat(actual)
                .as("%s | The actual: <%s> is equal to: <%s>", context, actual, expected)
                .isEqualTo(expected);
    }

    public static void fail(String errorMessage) {
        Assertions.fail(errorMessage);
    }
}
