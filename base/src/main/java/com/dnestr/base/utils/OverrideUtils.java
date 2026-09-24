package com.dnestr.base.utils;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Reflectively overwrites named fields on a config object from a string-keyed/string-valued map —
 * e.g. applying command-line or environment overrides onto an already-loaded config object without
 * needing a setter for every field. Supports only {@code String}, {@code Integer}/{@code int},
 * {@code Boolean}/{@code boolean}, and {@code Long}/{@code long} field types. Not currently called
 * from elsewhere in {@code test-core} itself (only exercised by its own test); available for a
 * consuming app's config loading to use.
 */
public final class OverrideUtils {

    private OverrideUtils() {}

    /**
     * For each entry in {@code overrides}, reflectively sets the declared field of that name on
     * {@code target} to the entry's value, converted to the field's actual type.
     *
     * @throws IllegalArgumentException if a field doesn't exist, isn't accessible, has an
     *                                   unsupported type, or the value can't be parsed as that type
     */
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