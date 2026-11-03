package com.dnestr.base.logs;

import lombok.experimental.UtilityClass;

/** ANSI colours for console banners, and the marker that starts every element action line. */
@UtilityClass
public class LogStyles {

    public final String RESET = "\u001B[0m";

    public final String GREEN = "\u001B[32m";
    public final String RED = "\u001B[31m";
    public final String YELLOW = "\u001B[33m";
    public final String BLUE = "\u001B[34m";

    public final String BOLD = "\u001B[1m";
    public final String GRAY = "\u001B[90m";

    /** Starts every element action line, so those lines stand out from the rest of the log. */
    public final String ACTION = "👉";
}
