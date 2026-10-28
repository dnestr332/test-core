package com.dnestr.base.utils;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Turns loosely-formatted human text (as typed in a Gherkin step, e.g. {@code "read-only"} or
 * {@code "Read Only"}) into an enum constant, via {@link #toEnumKey} normalization before
 * {@link Enum#valueOf}. Every method here is tolerant of the same messy input; they differ only in
 * what happens when the value doesn't match any constant.
 */
public class EnumUtils {

    private EnumUtils() {}

    /**
     * Normalizes {@code rawValue} via {@link #toEnumKey} and resolves it against {@code enumClass}.
     *
     * @throws IllegalArgumentException if {@code rawValue} is null/blank, or matches no constant
     *                                  of {@code enumClass} (message lists the available options)
     */
    public static <T extends Enum<T>> T parse(Class<T> enumClass, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException(
                    String.format("Value for %s cannot be null or blank.",
                            enumClass.getSimpleName())
            );
        }

        String key = toEnumKey(rawValue);

        try {
            return Enum.valueOf(enumClass, key);
        } catch (IllegalArgumentException e) {
            String available = Arrays.stream(enumClass.getEnumConstants())
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));

            String errorMessage = String.format(
                    "Invalid value '%s' for %s. Available options: [%s]",
                    rawValue,
                    enumClass.getSimpleName(),
                    available
            );
            throw new IllegalArgumentException(errorMessage);
        }
    }

    /** Like {@link #parse}, but returns {@code null} instead of throwing on a null/blank/unmatched value. */
    public static <T extends Enum<T>> T tryParse(Class<T> enumClass, String rawValue) {
        try {
            return parse(enumClass, rawValue);
        } catch (Exception ignored) {
            return null;
        }
    }

    /** Like {@link #tryParse}, but returns {@code defaultValue} instead of {@code null} when parsing fails. */
    public static <T extends Enum<T>> T parseOrDefault(Class<T> enumClass, String rawValue, T defaultValue) {
        T result = tryParse(enumClass, rawValue);
        return result != null ? result : defaultValue;
    }

    /** Whether {@code rawValue} would successfully resolve to a constant of {@code enumClass} via {@link #parse}. */
    public static <T extends Enum<T>> boolean isValid(Class<T> enumClass, String rawValue) {
        return tryParse(enumClass, rawValue) != null;
    }

    /**
     * Resolves {@code rawValue} against every enum among the permitted subclasses of the sealed {@code sealedType},
     * normalizing it via {@link #toEnumKey} first, and returns the first constant with that name. Constant names
     * must therefore be unique across those enums, or a later one is unreachable.
     *
     * @throws IllegalArgumentException if {@code sealedType} is not sealed, or no permitted enum has the constant
     */
    public static <T> T parseSealed(Class<T> sealedType, String rawValue) {
        if (!sealedType.isSealed()) {
            throw new IllegalArgumentException(sealedType.getSimpleName() + " is not a sealed type");
        }
        String key = toEnumKey(rawValue);
        for (Class<?> permitted : sealedType.getPermittedSubclasses()) {
            if (!permitted.isEnum()) continue;
            for (Object constant : permitted.getEnumConstants()) {
                if (((Enum<?>) constant).name().equals(key)) return sealedType.cast(constant);
            }
        }
        throw new IllegalArgumentException("Unknown %s: '%s'".formatted(sealedType.getSimpleName(), rawValue));
    }

    /**
     * Normalizes free-form text into an {@code Enum.valueOf}-compatible key: trims, uppercases,
     * collapses any run of non-alphanumeric characters into a single {@code _}, and strips a
     * leading/trailing {@code _}. E.g. {@code "  read - only! "} → {@code "READ_ONLY"}. Returns
     * {@code ""} for a {@code null} input.
     */
    public static String toEnumKey(String s) {
        return s == null ? "" :
                s.trim().toUpperCase()
                        .replaceAll("[^A-Z0-9]+", "_")
                        .replaceAll("_+", "_")
                        .replaceAll("^_|_$", "");
    }
}
