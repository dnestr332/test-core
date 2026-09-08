package com.dnestr.base.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ElementUtils {

    private ElementUtils() {}

    public static double getStringAsDouble(String raw) {
        Matcher matcher = Pattern.compile("-?\\d+(\\.\\d+)?").matcher(raw);
        if (matcher.find()) return Double.parseDouble(matcher.group());
        throw new IllegalArgumentException("No numeric value found in: " + raw);
    }

    public static double getStringAsDoubleReplaceCommas(String raw) {
        Matcher matcher = Pattern.compile("[\\d,]+(?:\\.\\d+)?").matcher(raw);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group().replace(",", ""));
        }
        throw new IllegalArgumentException("No numeric value found in: " + raw);
    }
}