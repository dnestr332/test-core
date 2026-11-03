package com.dnestr.mobile.utils;

import io.appium.java_client.AppiumDriver;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

import java.util.*;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayInputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import com.dnestr.mobile.context.TestContext;
import io.appium.java_client.remote.AppiumWebElement;
import lombok.experimental.UtilityClass;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;

import java.util.stream.Collectors;

/**
 * Reading and writing elements, and the texts the whole screen shows, accounting for how Android and iOS expose a
 * value or text differently.
 */
@UtilityClass
public class ElementUtils {

    /** Clicks the element, clears any existing value, then types {@code value} — the standard "replace a field's content" sequence. */
    public void updateValue(WebElement element, String value) {
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
    public String getActualText(WebElement element) {
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
    public List<String> getListOfText(List<WebElement> target) {
        return target.stream()
                .map(ElementUtils::getActualText)
                .map(String::trim)
                .collect(Collectors.toList());
    }

    /**
     * Clicks the element, deletes up to {@code charactersToDelete} characters with Backspace, then types
     * {@code value} — for an iOS field whose {@code clear()} leaves the old value in place.
     */
    public void replaceValueByBackspace(WebElement element, int charactersToDelete, String value) {
        element.click();
        element.sendKeys(Keys.BACK_SPACE.toString().repeat(charactersToDelete));
        element.sendKeys(value);
    }

    /** The trimmed {@link #getActualText} of the first element under {@code root} matching {@code child}; {@code ""} when none does. */
    public String childText(WebElement root, By child) {
        List<WebElement> matches = root.findElements(child);
        return matches.isEmpty() ? "" : getActualText(matches.getFirst());
    }

    /**
     * The element's id: its {@code resource-id} on Android, its {@code name} on iOS — where a React Native testID
     * lands on each. {@code ""} when it has none.
     */
    public String getId(WebElement element) {
        String id = element.getAttribute(TestContext.isIos() ? "name" : "resource-id");
        return id == null ? "" : id;
    }

    /** The element's WebDriver reference, which {@code mobile:} commands take as {@code elementId}. */
    public String referenceId(WebElement element) {
        return switch (element) {
            case AppiumWebElement appium -> appium.getId();
            case RemoteWebElement remote -> remote.getId();
            default -> throw new IllegalArgumentException(element.getClass().getName() + " is not a driver element");
        };
    }

    /** The center of the element's bounding box. */
    public Point centerOf(WebElement element) {
        return centerOf(element.getRect());
    }

    /** The center of {@code box}. */
    public Point centerOf(Rectangle box) {
        return new Point(box.getX() + box.getWidth() / 2, box.getY() + box.getHeight() / 2);
    }

    /**
     * The element whose vertical center is nearest {@code container}'s — the selected row of a wheel picker, which is
     * the one drawn across its center line. Empty for an empty list.
     */
    public Optional<WebElement> closestToCenterY(List<WebElement> elements, Rectangle container) {
        int centerY = centerOf(container).getY();
        return elements.stream()
                .min(Comparator.comparingInt(element -> Math.abs(centerOf(element).getY() - centerY)));
    }

    //region SCREEN CONTENT
    /**
     * Every text in the current page source, in document order and without repeats: Android's {@code text}
     * attributes, iOS's {@code label} and {@code value}. Covers screens of other apps (a dialer, a browser) too.
     */
    public List<String> getPageSourceTexts(AppiumDriver driver) {
        List<String> attributes = TestContext.isIos() ? List.of("label", "value") : List.of("text");
        Set<String> texts = new LinkedHashSet<>();
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            var document = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(Objects.
                            requireNonNull(driver.getPageSource()).getBytes(StandardCharsets.UTF_8)));
            NodeList nodes = document.getElementsByTagName("*");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element node = (Element) nodes.item(i);
                for (String attribute : attributes) {
                    String text = node.getAttribute(attribute).trim();
                    if (!text.isEmpty()) texts.add(text);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not read the page source", e);
        }
        return new ArrayList<>(texts);
    }
    //endregion
}
