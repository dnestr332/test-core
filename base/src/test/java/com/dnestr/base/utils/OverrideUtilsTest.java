package com.dnestr.base.utils;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OverrideUtilsTest {

    static class Config {
        int retries = 1;
        boolean headless = false;
        long timeoutMs = 1000L;
        String name = "default";
        double ratio = 1.0;
    }

    @Test
    void apply_overridesIntField() {
        Config config = new Config();
        OverrideUtils.apply(config, Map.of("retries", "5"));
        assertThat(config.retries).isEqualTo(5);
    }

    @Test
    void apply_overridesBooleanField() {
        Config config = new Config();
        OverrideUtils.apply(config, Map.of("headless", "true"));
        assertThat(config.headless).isTrue();
    }

    @Test
    void apply_overridesLongField() {
        Config config = new Config();
        OverrideUtils.apply(config, Map.of("timeoutMs", "9999"));
        assertThat(config.timeoutMs).isEqualTo(9999L);
    }

    @Test
    void apply_overridesStringField() {
        Config config = new Config();
        OverrideUtils.apply(config, Map.of("name", "custom"));
        assertThat(config.name).isEqualTo("custom");
    }

    @Test
    void apply_multipleFieldsAtOnce() {
        Config config = new Config();
        OverrideUtils.apply(config, Map.of("retries", "3", "headless", "true"));
        assertThat(config.retries).isEqualTo(3);
        assertThat(config.headless).isTrue();
    }

    @Test
    void apply_unknownField_wrapsInIllegalArgumentException() {
        Config config = new Config();
        assertThatThrownBy(() -> OverrideUtils.apply(config, Map.of("doesNotExist", "1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("doesNotExist");
    }

    @Test
    void apply_unsupportedFieldType_wrapsClearErrorAboutType() {
        Config config = new Config();
        assertThatThrownBy(() -> OverrideUtils.apply(config, Map.of("ratio", "1.5")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ratio")
                .cause()
                .hasMessageContaining("double");
    }
}
