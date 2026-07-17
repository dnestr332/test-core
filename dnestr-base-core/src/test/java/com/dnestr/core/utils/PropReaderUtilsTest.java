package com.dnestr.core.utils;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropReaderUtilsTest {

    @Test
    void load_missingResource_throwsIllegalState() {
        assertThatThrownBy(() -> PropReaderUtils.load("does-not-exist.properties"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("does-not-exist.properties");
    }

    @Test
    void load_existingResource_readsValues() {
        Properties props = PropReaderUtils.load("test.properties");
        assertThat(props.getProperty("greeting")).isEqualTo("hello");
    }

    @Test
    void getProperty_systemPropertyTakesPrecedence() {
        Properties props = new Properties();
        props.setProperty("app.key", "from-file");
        System.setProperty("app.key", "from-system");
        try {
            assertThat(PropReaderUtils.getProperty(props, "app.key")).isEqualTo("from-system");
        } finally {
            System.clearProperty("app.key");
        }
    }

    @Test
    void getProperty_fallsBackToFileWhenNoSystemPropertyOrEnv() {
        Properties props = new Properties();
        props.setProperty("app.only.in.file", "from-file");
        assertThat(PropReaderUtils.getProperty(props, "app.only.in.file")).isEqualTo("from-file");
    }

    @Test
    void getProperty_missingEverywhere_returnsNull() {
        Properties props = new Properties();
        assertThat(PropReaderUtils.getProperty(props, "totally.unknown.key")).isNull();
    }
}
