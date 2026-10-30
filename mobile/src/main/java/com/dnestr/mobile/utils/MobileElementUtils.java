package com.dnestr.mobile.utils;

import com.dnestr.mobile.context.TestContext;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** Element read/write helpers that account for Android/iOS differences in how a value or text is actually exposed on a {@link WebElement}. */
public final class MobileElementUtils {

    private MobileElementUtils() {}

    /** Clicks the element, clears any existing value, then types {@code value} — the standard "replace a field's content" sequence. */
    public static void updateValue(WebElement element, String value) {
        element.click();
        element.clear();
        element.sendKeys(value);
    }

    /**
     * Returns an element's displayed text, platform-appropriately: on iOS, {@code getText()} is
     * unreliable for many native controls, so this reads the {@code value} accessibility attribute
     * first, falling back to {@code label}, and finally {@code ""} if neither is set; on Android,
     * {@code element.getText()} is used directly.
     */
    public static String getActualText(WebElement element) {
        if (TestContext.isIos()) {
            String value = element.getAttribute("value");
            if (value != null && !value.isBlank()) {
                return value.trim();
            }

            String label = element.getAttribute("label");
            if (label != null && !label.isBlank()) {
                return label.trim();
            }

            return "";
        } else {
            return element.getText().trim();
        }
    }

    /** {@link #getActualText} applied to every element in {@code target}, in order. */
    public static List<String> getListOfText(List<WebElement> target) {
        return target.stream()
                .map(MobileElementUtils::getActualText)
                .map(String::trim)
                .collect(Collectors.toList());
    }

    /**
     * Clicks the element, deletes up to {@code charactersToDelete} characters with Backspace, then types
     * {@code value} — for an iOS field whose {@code clear()} leaves the old value in place.
     */
    public static void replaceValueByBackspace(WebElement element, int charactersToDelete, String value) {
        element.click();
        element.sendKeys(Keys.BACK_SPACE.toString().repeat(charactersToDelete));
        element.sendKeys(value);
    }

    /** The trimmed {@link #getActualText} of the first element under {@code root} matching {@code child}; {@code ""} when none does. */
    public static String childText(WebElement root, By child) {
        List<WebElement> matches = root.findElements(child);
        return matches.isEmpty() ? "" : getActualText(matches.getFirst());
    }

    /**
     * The element's id: its {@code resource-id} on Android, its {@code name} on iOS — where a React Native testID
     * lands on each. {@code ""} when it has none.
     */
    public static String getId(WebElement element) {
        String id = element.getAttribute(TestContext.isIos() ? "name" : "resource-id");
        return id == null ? "" : id;
    }

    /** The center of the element's bounding box. */
    public static Point centerOf(WebElement element) {
        return centerOf(element.getRect());
    }

    /** The center of {@code box}. */
    public static Point centerOf(Rectangle box) {
        return new Point(box.getX() + box.getWidth() / 2, box.getY() + box.getHeight() / 2);
    }

    /**
     * The element whose vertical center is nearest {@code container}'s — the selected row of a wheel picker, which is
     * the one drawn across its center line. Empty for an empty list.
     */
    public static Optional<WebElement> closestToCenterY(List<WebElement> elements, Rectangle container) {
        int centerY = centerOf(container).getY();
        return elements.stream()
                .min(Comparator.comparingInt(element -> Math.abs(centerOf(element).getY() - centerY)));
    }
}
