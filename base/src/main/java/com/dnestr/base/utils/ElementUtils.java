package com.dnestr.base.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pulls a numeric value out of arbitrary UI text (e.g. {@code "Total: $1,234.56"}) that isn't already a clean number string. */
public final class ElementUtils {

    private ElementUtils() {}

    /**
     * Finds and parses the first (optionally negative, optionally decimal) number in {@code raw},
     * e.g. {@code "Price: -42.5 USD"} → {@code -42.5}.
     *
     * @throws IllegalArgumentException if {@code raw} contains no numeric substring
     */
    public static double getStringAsDouble(String raw) {
        Matcher matcher = Pattern.compile("-?\\d+(\\.\\d+)?").matcher(raw);
        if (matcher.find()) return Double.parseDouble(matcher.group());
        throw new IllegalArgumentException("No numeric value found in: " + raw);
    }

    /**
     * Like {@link #getStringAsDouble}, but for numbers written with thousands separators, e.g.
     * {@code "1,234.56"} → {@code 1234.56}. Does not handle a leading minus sign.
     *
     * @throws IllegalArgumentException if {@code raw} contains no numeric substring
     */
    public static double getStringAsDoubleReplaceCommas(String raw) {
        Matcher matcher = Pattern.compile("[\\d,]+(?:\\.\\d+)?").matcher(raw);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group().replace(",", ""));
        }
        throw new IllegalArgumentException("No numeric value found in: " + raw);
    }
}