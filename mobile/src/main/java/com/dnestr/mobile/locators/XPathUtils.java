package com.dnestr.mobile.locators;

import java.util.ArrayList;
import java.util.List;

/** Builds XPath expressions from arbitrary text. */
public final class XPathUtils {

    private XPathUtils() {}

    /**
     * {@code text} as an XPath string literal: single-quoted, double-quoted when it holds a single quote, and a
     * {@code concat()} of both kinds when it holds both, which no plain literal can express.
     */
    public static String literal(String text) {
        if (!text.contains("'")) return "'" + text + "'";
        if (!text.contains("\"")) return "\"" + text + "\"";

        List<String> parts = new ArrayList<>();
        for (String piece : text.split("'", -1)) {
            parts.add("'" + piece + "'");
            parts.add("\"'\"");
        }
        parts.removeLast();
        return "concat(" + String.join(", ", parts) + ")";
    }
}
