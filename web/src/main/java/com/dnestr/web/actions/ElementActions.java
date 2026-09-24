package com.dnestr.web.actions;

import com.dnestr.web.logs.FailureCatcher;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.WaitForSelectorState;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.stream.IntStream;

import static com.dnestr.base.actions.ElementAction.*;

/**
 * Playwright-backed element actions used across page objects. Every action that touches a
 * {@link Locator} or the {@link Page} is routed through {@link FailureCatcher}, which logs the
 * step, times it, and — on failure — records the throwable to {@code TestFailureContext} before
 * rethrowing, so callers don't need their own try/catch for diagnostics.
 * <p>
 * Used by composition, not inheritance: page objects hold an instance (typically passed into
 * {@code BasePage}'s constructor) rather than extending this class. Keep additions here generic
 * and Playwright-only; push domain knowledge (specific locators, flows, timeouts) into the page
 * objects that use it instead.
 */
@Slf4j
@RequiredArgsConstructor
public class ElementActions {

    private final Page page;
    private final FailureCatcher failureCatcher;

    /** Clicks the locator. Rethrows (after failure capture) if the click fails, e.g. the element is not clickable. */
    public void click(Locator locator) {
        failureCatcher.withFailureCapture(CLICK, locator,
                () -> locator.click()
        );
    }

    /** Fills the locator's value directly (bypasses keystroke events), replacing any existing content. */
    public void type(Locator locator, String text) {
        failureCatcher.withFailureCapture(TYPE, locator, text,
                () -> locator.fill(text)
        );
    }

    /** Clicks, clears, then types {@code text} via real keyboard events, for fields that need input listeners to fire per keystroke. */
    public void typeWithKeyboard(Locator locator, String text) {
        failureCatcher.withFailureCapture(TYPE, locator, text, () -> {
            locator.click();
            locator.clear();
            page.keyboard().type(text);
        });
    }

    /** Clicks, selects all existing content (Ctrl+A), deletes it, then fills in {@code text}. Use when {@link #type} alone doesn't clear a field reliably. */
    public void clearAndType(Locator locator, String text) {
        failureCatcher.withFailureCapture(TYPE, locator, text, () -> {
            locator.click();
            locator.press("Control+A");
            locator.press("Backspace");
            locator.fill(text);
        });
    }

    /** Presses a single key or key combination (e.g. {@code "Enter"}, {@code "Control+A"}) on the locator. */
    public void press(Locator locator, String key) {
        failureCatcher.withFailureCapture(TYPE, locator, key,
                () -> locator.press(key)
        );
    }

    /**
     * Returns the locator's trimmed visible text, falling back to its input value for form
     * controls whose text doesn't come through {@code innerText} (e.g. {@code <input>}), and to
     * an empty string if both are blank.
     */
    public String text(Locator locator) {
        return failureCatcher.withFailureCapture(GET_TEXT, locator,
                () -> {
                    String text = locator.innerText();
                    if (text != null && !text.isBlank()) {
                        return text.trim();
                    }
                    String value = locator.inputValue();
                    if (value != null && !value.isBlank()) {
                        return value.trim();
                    }
                    return "";
                }
        );
    }

    /** Returns the trimmed text content of every element matched by the locator, in document order. */
    public List<String> getTexts(Locator locator) {
        return failureCatcher.withFailureCapture(GET_TEXT, locator,
                () -> locator.allTextContents()
                        .stream()
                        .map(String::trim)
                        .toList()
        );
    }

    /**
     * Returns the given attribute's trimmed value for every element matched by the locator, in
     * document order; an element missing the attribute contributes {@code null} at its position.
     */
    public List<String> getAttributes(Locator locator, String attribute) {
        return failureCatcher.withFailureCapture(GET_TEXT, locator,
                () -> IntStream.range(0, locator.count())
                        .mapToObj(i -> locator.nth(i).getAttribute(attribute))
                        .map(val -> val != null ? val.trim() : null)
                        .toList()
        );
    }

    /** Scrolls the locator into the viewport if it isn't already fully visible. */
    public void scrollIntoView(Locator locator) {
        failureCatcher.withFailureCapture(SCROLL, locator,
                () -> locator.scrollIntoViewIfNeeded());
    }

    /** Scrolls the page to the very top ({@code window.scrollTo(0, 0)}). */
    public void scrollToTop() {
        failureCatcher.withFailureCapture(SCROLL,
                () -> page.evaluate("window.scrollTo(0, 0)")
        );
    }

    /** Scrolls the page to the very bottom of {@code document.body}. */
    public void scrollToBottom() {
        failureCatcher.withFailureCapture(SCROLL,
                () -> page.evaluate("window.scrollTo(0, document.body.scrollHeight)")
        );
    }

    /** Scrolls the window by the given relative offset ({@code window.scrollBy(x, y)}). */
    public void scrollBy(int x, int y) {
        failureCatcher.withFailureCapture(SCROLL,
                () -> page.evaluate(
                        "args => window.scrollBy(args.x, args.y)",
                        Map.of("x", x, "y", y))
        );
    }

    /** Moves the mouse over the locator, triggering hover state/effects without clicking. */
    public void hover(Locator locator) {
        failureCatcher.withFailureCapture(HOVER, locator,
                () -> locator.hover()
        );
    }

    /**
     * Zooms the whole page by scaling {@code document.documentElement} with a CSS transform and
     * compensating its width so layout still fills the viewport. Unlike {@link #setCssZoom}, this
     * works in every browser engine (including WebKit/Firefox, where CSS {@code zoom} is unsupported
     * or behaves inconsistently), at the cost of being a visual transform rather than true zoom.
     *
     * @param scale 1.0 = 100%, 1.5 = 150%, etc.
     */
    public void setZoom(double scale) {
        failureCatcher.withFailureCapture(ZOOM,
                () -> page.evaluate("""
                        scale => {
                            document.documentElement.style.transformOrigin = '0 0';
                            document.documentElement.style.transform = 'scale(' + scale + ')';
                            document.documentElement.style.width = (100 / scale) + '%';
                        }
                        """, scale)
        );
    }

    /**
     * Zooms the page via the native CSS {@code zoom} property on {@code document.body}. Simpler
     * and more "real" than {@link #setZoom} (reflows layout like actual browser zoom would), but
     * only reliably supported in Chromium-based engines.
     *
     * @param scale 1.0 = 100%, 1.5 = 150%, etc.
     */
    public void setCssZoom(double scale) {
        failureCatcher.withFailureCapture(ZOOM,
                () -> page.evaluate("scale => { document.body.style.zoom = String(scale); }", scale)
        );
    }

    /**
     * Polls {@code condition} until it becomes true or {@code timeoutMs} elapses. Unlike raw
     * {@link Page#waitForCondition}, this never throws on timeout — it simply returns {@code false} —
     * which makes it a good fit for flows that can resolve more than one way (e.g. a login that
     * either succeeds or shows an error toast), where the caller wants to inspect state afterward
     * rather than branch on a caught exception. Not routed through {@link FailureCatcher}: a
     * timeout here is an expected outcome, not a test failure, so nothing is logged as an error.
     *
     * @param condition predicate evaluated by Playwright until it returns {@code true}
     * @param timeoutMs maximum time to wait, in milliseconds
     * @return {@code true} if the condition became true in time, {@code false} if it timed out
     */
    public boolean waitForCondition(BooleanSupplier condition, int timeoutMs) {
        try {
            page.waitForCondition(condition, new Page.WaitForConditionOptions().setTimeout(timeoutMs));
            return true;
        } catch (TimeoutError timedOut) {
            return false;
        }
    }

    /**
     * Waits up to {@code timeoutMs} for the locator to become visible, returning {@code false}
     * instead of throwing if it never does. Use for steps that may or may not appear — e.g. a
     * consent dialog shown only on the first login of a session.
     *
     * @return {@code true} if the locator became visible in time, {@code false} if it timed out
     */
    public boolean isVisibleWithinTimeout(Locator locator, int timeoutMs) {
        return waitForState(locator, WaitForSelectorState.VISIBLE, timeoutMs);
    }

    /**
     * Waits up to {@code timeoutMs} for the locator to become hidden or detached, returning
     * {@code false} instead of throwing if it's still visible when time runs out. Use to wait out
     * a spinner or overlay that may already be gone by the time this is called.
     *
     * @return {@code true} if the locator became hidden in time, {@code false} if it timed out
     */
    public boolean isHiddenWithinTimeout(Locator locator, int timeoutMs) {
        return waitForState(locator, WaitForSelectorState.HIDDEN, timeoutMs);
    }

    /**
     * Clicks the locator if it becomes visible within {@code timeoutMs}; otherwise does nothing.
     * Intended for optional one-off steps in a flow (consent dialogs, promo banners) that a
     * scenario shouldn't fail on just because they didn't appear this time.
     *
     * @return {@code true} if the locator appeared and was clicked, {@code false} if it timed out
     */
    public boolean clickIfVisible(Locator locator, int timeoutMs) {
        if (!isVisibleWithinTimeout(locator, timeoutMs)) {
            return false;
        }
        click(locator);
        return true;
    }

    private boolean waitForState(Locator locator, WaitForSelectorState state, int timeoutMs) {
        try {
            locator.waitFor(new Locator.WaitForOptions().setState(state).setTimeout(timeoutMs));
            return true;
        } catch (TimeoutError timedOut) {
            return false;
        }
    }

    private static final int DEFAULT_DEBOUNCE_MS = 400;
    private static final int DEFAULT_MAX_WAIT_MS = 5000;
    private static final int TIMEOUT_BUFFER_MS = 5000;

    /** {@link #waitForStability(int, int)} with a 400ms debounce and a 5s overall cap. */
    public void waitForStability() {
        waitForStability(DEFAULT_DEBOUNCE_MS, DEFAULT_MAX_WAIT_MS);
    }

    /**
     * Waits for the DOM to go quiet: attaches a {@code MutationObserver} to {@code document.body}
     * (child list, attributes, and text changes, recursively) and resolves once {@code debounceMs}
     * has passed with no further mutation. Useful after an action that triggers async re-rendering
     * (e.g. a client-side navigation or a component fetching data) where there's no single locator
     * to assert on.
     * <p>
     * Resolution is guaranteed within {@code maxWaitMs} even under continuous mutation (animations,
     * polling widgets, ads) — an absolute cap independent of the debounce timer fires regardless, so
     * this method returns normally rather than throwing a Playwright {@code TimeoutError} in that
     * case. The underlying observer is always disconnected before returning.
     *
     * @param debounceMs quiet period required, in milliseconds, before the DOM is considered stable
     * @param maxWaitMs  upper bound, in milliseconds, on the total wait regardless of ongoing mutation
     */
    public void waitForStability(int debounceMs, int maxWaitMs) {
        log.info("Waiting for DOM stability (debounce={}ms, maxWait={}ms)...", debounceMs, maxWaitMs);

        page.waitForFunction("""
                        ({debounceMs, maxWaitMs}) => {
                            return new Promise(resolve => {
                                let settled = false;
                                let debounceTimer;

                                const finish = () => {
                                    if (settled) return;
                                    settled = true;
                                    clearTimeout(debounceTimer);
                                    clearTimeout(maxWaitTimer);
                                    observer.disconnect();
                                    resolve(true);
                                };

                                const observer = new MutationObserver(() => {
                                    clearTimeout(debounceTimer);
                                    debounceTimer = setTimeout(finish, debounceMs);
                                });
                                observer.observe(document.body, {
                                    childList: true,
                                    subtree: true,
                                    attributes: true,
                                    characterData: true
                                });

                                const maxWaitTimer = setTimeout(finish, maxWaitMs);
                                debounceTimer = setTimeout(finish, debounceMs);
                            });
                        }
                        """,
                Map.of("debounceMs", debounceMs, "maxWaitMs", maxWaitMs),
                new Page.WaitForFunctionOptions().setTimeout(maxWaitMs + TIMEOUT_BUFFER_MS)
        );
    }
}
