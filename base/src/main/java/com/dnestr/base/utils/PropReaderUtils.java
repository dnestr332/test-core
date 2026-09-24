package com.dnestr.base.utils;

import java.io.InputStream;
import java.util.Properties;

/** Loads a {@code .properties} file from the classpath and resolves individual keys with system-property/env-var override precedence. */
public final class PropReaderUtils {

    private PropReaderUtils() {}

    /**
     * Loads the classpath resource at {@code filePath} (via the current thread's context class
     * loader) as a {@link Properties} object.
     *
     * @throws RuntimeException wrapping an {@link IllegalStateException} if the resource doesn't
     *                          exist, or any other error encountered while reading it
     */
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

    /**
     * Resolves {@code key}, checked in order: a JVM system property named {@code key}; then an
     * environment variable named {@code key} uppercased with {@code .} replaced by {@code _} (e.g.
     * {@code "app.baseUrl"} → {@code APP_BASEURL}); then {@code key} looked up in {@code props}
     * itself. A blank system property or env var value is treated as unset and falls through to
     * the next source. Lets a value baked into a checked-in properties file be overridden per-run
     * without editing the file.
     */
    public static String getProperty(Properties props, String key) {
        String systemValue = System.getProperty(key);
        if (systemValue != null && !systemValue.isBlank()) return systemValue;

        String envKey = key.toUpperCase().replace('.', '_');
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) return envValue;

        return props.getProperty(key);
    }
}