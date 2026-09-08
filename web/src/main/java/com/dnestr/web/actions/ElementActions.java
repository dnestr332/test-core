package com.dnestr.web.actions;

import com.dnestr.web.logs.FailureCatcher;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static com.dnestr.base.actions.ElementAction.*;

@Slf4j
@RequiredArgsConstructor
public class ElementActions {

    private final Page page;
    private final FailureCatcher failureCatcher;

    //region ACTIONS
    public void click(Locator locator) {
        failureCatcher.withFailureCapture(CLICK, locator,
                () -> locator.click()
        );
    }

    public void type(Locator locator, String text) {
        failureCatcher.withFailureCapture(TYPE, locator, text,
                () -> locator.fill(text)
        );
    }

    public void typeWithKeyboard(Locator locator, String text) {
        failureCatcher.withFailureCapture(TYPE, locator, text, () -> {
            locator.click();
            locator.clear();
            page.keyboard().type(text);
        });
    }

    public void clearAndType(Locator locator, String text) {
        failureCatcher.withFailureCapture(TYPE, locator, text, () -> {
            locator.click();
            locator.press("Control+A");
            locator.press("Backspace");
            locator.fill(text);
        });
    }

    public void press(Locator locator, String key) {
        failureCatcher.withFailureCapture(TYPE, locator, key,
                () -> locator.press(key)
        );
    }

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

    public List<String> getTexts(Locator locator) {
        return failureCatcher.withFailureCapture(GET_TEXT, locator,
                () -> locator.allTextContents()
                        .stream()
                        .map(String::trim)
                        .toList()
        );
    }

    public List<String> getAttributes(Locator locator, String attribute) {
        return failureCatcher.withFailureCapture(GET_TEXT, locator,
                () -> IntStream.range(0, locator.count())
                        .mapToObj(i -> locator.nth(i).getAttribute(attribute))
                        .map(val -> val != null ? val.trim() : null)
                        .toList()
        );
    }
    //endregion

    //region SCROLLS & ZOOMS
    public void scrollIntoView(Locator locator) {
        failureCatcher.withFailureCapture(SCROLL, locator,
                () -> locator.scrollIntoViewIfNeeded());
    }

    public void scrollToTop() {
        failureCatcher.withFailureCapture(SCROLL,
                () -> page.evaluate("window.scrollTo(0, 0)")
        );
    }

    public void scrollToBottom() {
        failureCatcher.withFailureCapture(SCROLL,
                () -> page.evaluate("window.scrollTo(0, document.body.scrollHeight)")
        );
    }

    public void scrollBy(int x, int y) {
        failureCatcher.withFailureCapture(SCROLL,
                () -> page.evaluate(
                        "args => window.scrollBy(args.x, args.y)",
                        Map.of("x", x, "y", y))
        );
    }

    public void hover(Locator locator) {
        failureCatcher.withFailureCapture(HOVER, locator,
                () -> locator.hover()
        );
    }

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
    //endregion

    //region STABILIZATION
    public void waitForStability() {
        log.info("Waiting for DOM stability...");

        page.waitForFunction("""
                () => {
                    return new Promise(resolve => {
                        let timer;
                
                        const finish = (observer) => {
                            observer.disconnect();
                            resolve(true);
                        };
                        const observer = new MutationObserver(() => {
                            clearTimeout(timer);
                            timer = setTimeout(() => finish(observer), 400);
                        });
                        observer.observe(document.body, {
                            childList: true,
                            subtree: true,
                            attributes: true
                        });
                        timer = setTimeout(() => finish(observer), 400);
                    });
                }
                """
        );
    }
    //endregion
}
