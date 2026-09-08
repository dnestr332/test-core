package com.dnestr.base.utils;

import java.io.InputStream;
import java.util.Properties;

public final class PropReaderUtils {

    private PropReaderUtils() {}

    public static Properties load(String filePath) {
        try (InputStream is = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(filePath)) {

            if (is == null) {
                throw new IllegalStateException("Resource not found: " + filePath);
            }

            Properties props = new Properties();
            props.load(is);
            return props;

        } catch (Exception e) {
            throw new RuntimeException("Failed to load: " + filePath, e);
        }
    }

    public static String getProperty(Properties props, String key) {
        String systemValue = System.getProperty(key);
        if (systemValue != null && !systemValue.isBlank()) return systemValue;

        String envKey = key.toUpperCase().replace('.', '_');
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) return envValue;

        return props.getProperty(key);
    }
}