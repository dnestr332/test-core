package com.dnestr.base.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads values out of text as a UI shows it (e.g. {@code "Total: $1,234.56"}): numbers, digits and dates. */
public final class TextUtils {

    /** A year letter outside quoted literal text; a defaulted year would clash with a parsed one. */
    private static final Pattern HAS_YEAR = Pattern.compile("^(?:[^']|'[^']*')*?[yuY]");

    private TextUtils() {}

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

    /**
     * The number after the last {@code marker} in {@code raw}, e.g. the total in {@code "Was $12.00, now $9.50"} with
     * {@code '$'} → {@code 9.5}. {@link #getStringAsDouble} alone would read the first number.
     *
     * @throws IllegalArgumentException if {@code raw} has no {@code marker}, or no number follows the last one
     */
    public static double getAmountAfterLast(String raw, char marker) {
        int at = raw.lastIndexOf(marker);
        if (at < 0) {
            throw new IllegalArgumentException("No '%s' in: %s".formatted(marker, raw));
        }
        return getStringAsDoubleReplaceCommas(raw.substring(at + 1));
    }

    /**
     * The last {@code count} digits of {@code raw}, ignoring every other character, e.g. a card number's last four
     * or a phone number's last ten; all of its digits when it has fewer.
     *
     * @throws IllegalArgumentException if {@code count} is negative
     */
    public static String lastDigits(String raw, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count must not be negative, was " + count);
        }
        String digits = raw == null ? "" : raw.replaceAll("\\D", "");
        return digits.substring(Math.max(0, digits.length() - count));
    }

    /**
     * Parses {@code raw} with the first of {@code patterns} that fits, case-insensitively and with runs of whitespace
     * collapsed, taking {@code defaultYear} when the pattern has no year, e.g. {@code "Mon, Jan 5, 9:30 AM"} with
     * {@code "EEE, MMM d, h:mm a"}. Empty when {@code raw} is null or blank, or no pattern fits.
     */
    public static Optional<LocalDateTime> parseDateTime(String raw, Locale locale, int defaultYear, String... patterns) {
        if (raw == null || raw.isBlank()) return Optional.empty();

        String normalized = raw.trim().replaceAll("\\s+", " ");
        for (String pattern : patterns) {
            DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(pattern);
            if (!HAS_YEAR.matcher(pattern).find()) {
                builder.parseDefaulting(ChronoField.YEAR, defaultYear);
            }
            DateTimeFormatter formatter = builder.toFormatter(locale);
            try {
                return Optional.of(LocalDateTime.parse(normalized, formatter));
            } catch (DateTimeParseException notThisPattern) {
                // the next pattern may fit
            }
        }
        return Optional.empty();
    }
}
