package com.dnestr.core.utils;

import java.lang.reflect.Field;
import java.util.Map;

public final class OverrideUtils {

    private OverrideUtils() {}

    public static void apply(Object target, Map<String, String> overrides) {
        overrides.forEach((fieldName, value) -> {
            try {
                Field field = target.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                Object converted = getObject(value, field);
                field.set(target, converted);
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "Unable to override field: " + fieldName, e);
            }
        });
    }

    private static Object getObject(String value, Field field) {
        Class<?> type = field.getType();

        if (type == Integer.class || type == int.class) return Integer.parseInt(value);
        if (type == Boolean.class || type == boolean.class) return Boolean.parseBoolean(value);
        if (type == Long.class || type == long.class) return Long.parseLong(value);
        if (type == String.class) return value;

        throw new IllegalArgumentException(
                "Unsupported field type for override: " + type.getName());
    }
}