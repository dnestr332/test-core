package com.dnestr.base.logs;

/**
 * ANSI color codes, emoji status icons, and their plain-text ({@code [OK]}/{@code [FAIL]}/etc.)
 * fallbacks used by {@link BasePrettyPrinter} to format step log lines. Purely a constants holder.
 */
public final class LogStyles {

    private LogStyles() {}

    /**
     * ========== {@code COLORS} ==========
     */

    public static final String RESET  = "\u001B[0m";

    public static final String GREEN  = "\u001B[32m";
    public static final String RED    = "\u001B[31m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE   = "\u001B[34m";

    public static final String BOLD   = "\u001B[1m";
    public static final String GRAY   = "\u001B[90m";

    /**
     * ========== {@code ASCII STATUS ICONS} ==========
     */

    public static final String OK       = "😮‍💨";
    public static final String FAIL     = "❌";
    public static final String WARN     = "⚠️";
    public static final String INFO     = "ℹ️";

    public static final String OK_SHORT   = "[OK]";
    public static final String FAIL_SHORT = "[FAIL]";
    public static final String WARN_SHORT = "[WARN]";
    public static final String INFO_SHORT = "[INFO]";

    public static final String CLICK    = "👉";
    public static final String FIND     = "🔍";
    public static final String WAIT     = "⏳";
    public static final String RETRY    = "🔁";

    /**
     * ========== {@code ASCII memes} ==========
     */

    public static final String LENNY        = "😏";
    public static final String SHRUG        = "🤷";
    public static final String TABLE_FLIP = "🤬";
    public static final String UNFLIP       = "😌";
    public static final String BEAR         = "🐻";
    public static final String GHOST        = "👻";
    public static final String SUSPECT      = "🤨";
    public static final String HAPPY        = "😊";
    public static final String SAD          = "😢";
    public static final String HYPE         = "🚀";
    public static final String WOW          = "😲";
}
