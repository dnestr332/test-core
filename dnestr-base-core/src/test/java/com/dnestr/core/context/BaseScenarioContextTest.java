package com.dnestr.core.context;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BaseScenarioContextTest {

    enum Key { USER, TOKEN }

    static class Context extends BaseScenarioContext<Key> {}

    private final Context context = new Context();

    @Test
    void typedKey_setGetContainsRemove() {
        context.set(Key.USER, "alice");
        assertThat(context.contains(Key.USER)).isTrue();
        assertThat((String) context.get(Key.USER)).isEqualTo("alice");

        context.remove(Key.USER);
        assertThat(context.contains(Key.USER)).isFalse();
        assertThat((String) context.get(Key.USER)).isNull();
    }

    @Test
    void dynamicStringKey_setGetContainsRemove() {
        context.set("session-id", "abc-123");
        assertThat(context.contains("session-id")).isTrue();
        assertThat((String) context.get("session-id")).isEqualTo("abc-123");

        context.remove("session-id");
        assertThat(context.contains("session-id")).isFalse();
    }

    @Test
    void typedAndDynamicMaps_areIndependent() {
        context.set(Key.TOKEN, "typed-value");
        context.set("TOKEN", "dynamic-value");

        assertThat((String) context.get(Key.TOKEN)).isEqualTo("typed-value");
        assertThat((String) context.get("TOKEN")).isEqualTo("dynamic-value");
    }

    @Test
    void clear_wipesBothMaps() {
        context.set(Key.USER, "alice");
        context.set("dynamic", "value");

        context.clear();

        assertThat(context.contains(Key.USER)).isFalse();
        assertThat(context.contains("dynamic")).isFalse();
    }
}
