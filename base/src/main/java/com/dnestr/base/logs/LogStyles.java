package com.dnestr.base.logs;

import lombok.experimental.UtilityClass;

/**
 * ANSI color codes, emoji status icons, and their plain-text ({@code [OK]}/{@code [FAIL]}/etc.)
 * fallbacks used by {@link PrettyPrinter} to format step log lines. Purely a constants holder.
 */
@UtilityClass
public class LogStyles {

    /**
     * ========== {@code COLORS} ==========
     */

    public final String RESET  = "\u001B[0m";

    public final String GREEN  = "\u001B[32m";
    public final String RED    = "\u001B[31m";
    public final String YELLOW = "\u001B[33m";
    public final String BLUE   = "\u001B[34m";

    public final String BOLD   = "\u001B[1m";
    public final String GRAY   = "\u001B[90m";

    /**
     * ========== {@code ASCII STATUS ICONS} ==========
     */

    public final String OK       = "😮‍💨";
    public final String FAIL     = "❌";
    public final String WARN     = "⚠️";
    public final String INFO     = "ℹ️";

    public final String OK_SHORT   = "[OK]";
    public final String FAIL_SHORT = "[FAIL]";
    public final String WARN_SHORT = "[WARN]";
    public final String INFO_SHORT = "[INFO]";

    public final String CLICK    = "👉";
    public final String FIND     = "🔍";
    public final String WAIT     = "⏳";
    public final String RETRY    = "🔁";

    /**
     * ========== {@code ASCII memes} ==========
     */

    public final String LENNY        = "😏";
    public final String SHRUG        = "🤷";
    public final String TABLE_FLIP   = "🤬";
    public final String UNFLIP       = "😌";
    public final String BEAR         = "🐻";
    public final String GHOST        = "👻";
    public final String SUSPECT      = "🤨";
    public final String HAPPY        = "😊";
    public final String SAD          = "😢";
    public final String HYPE         = "🚀";
    public final String WOW          = "😲";
}
