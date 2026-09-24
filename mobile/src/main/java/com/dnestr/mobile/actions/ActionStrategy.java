package com.dnestr.mobile.actions;

/**
 * How aggressively {@code ElementActions} should try to find/interact with an element, from
 * fastest-but-least-robust to slowest-but-most-robust:
 * <ul>
 *   <li>{@code DEFAULT} — the normal path: wait for the element via {@code DriverWait}, falling
 *       back to a native click if the standard click fails.</li>
 *   <li>{@code FAST_TRY} — like {@code DEFAULT} but waits with the shorter timeout before falling
 *       back, for a call site that expects the element to already be there.</li>
 *   <li>{@code HARD_WAIT} — pauses for a fixed settle time ({@code FallbackActions#pause}) before
 *       relocating the element from scratch, for elements that only stabilize after some UI
 *       transition finishes; the most expensive strategy, used as a last resort.</li>
 *   <li>{@code NO_WAIT} — not itself dispatched on; used only as a label when logging that a
 *       fallback path (a native click/find with no further waiting) was taken.</li>
 * </ul>
 */
public enum ActionStrategy {

    DEFAULT,
    NO_WAIT,
    FAST_TRY,
    HARD_WAIT
}
