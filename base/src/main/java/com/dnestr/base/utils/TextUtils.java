package com.dnestr.base.utils;

import lombok.experimental.UtilityClass;

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
@UtilityClass
public class TextUtils {

    private final Pattern NUMBER = Pattern.compile("-?(?:\\d{1,3}(?:,\\d{3})+(?!\\d)|\\d+)(?:\\.\\d+)?");
    /** A year letter outside quoted literal text; a defaulted year would clash with a parsed one. */
    private final Pattern HAS_YEAR = Pattern.compile("^(?:[^']|'[^']*')*?[yuY]");

    /**
     * Finds and parses the first number in {@code raw}: optionally negative, optionally decimal, with or without
     * thousands separators, e.g. {@code "Balance: -1,234.56 USD"} → {@code -1234.56}. A comma only counts as a
     * separator before a group of three digits, so {@code "3, 4"} reads as {@code 3}.
     *
     * @throws IllegalArgumentException if {@code raw} contains no numeric substring
     */
    public double getStringAsDouble(String raw) {
        Matcher matcher = NUMBER.matcher(raw);
        if (matcher.find()) return Double.parseDouble(matcher.group().replace(",", ""));
        throw new IllegalArgumentException("No numeric value found in: " + raw);
    }

    /**
     * The number after the last {@code marker} in {@code raw}, e.g. the total in {@code "Was $12.00, now $9.50"} with
     * {@code '$'} → {@code 9.5}. {@link #getStringAsDouble} alone would read the first number.
     *
     * @throws IllegalArgumentException if {@code raw} has no {@code marker}, or no number follows the last one
     */
    public double getAmountAfterLast(String raw, char marker) {
        int at = raw.lastIndexOf(marker);
        if (at < 0) {
            throw new IllegalArgumentException("No '%s' in: %s".formatted(marker, raw));
        }
        return getStringAsDouble(raw.substring(at + 1));
    }

    /**
     * The last {@code count} digits of {@code raw}, ignoring every other character, e.g. a card number's last four
     * or a phone number's last ten; all of its digits when it has fewer.
     *
     * @throws IllegalArgumentException if {@code count} is negative
     */
    public String lastDigits(String raw, int count) {
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
    public Optional<LocalDateTime> parseDateTime(String raw, Locale locale, int defaultYear, String... patterns) {
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
