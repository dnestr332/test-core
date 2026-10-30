package com.dnestr.mobile.utils;

import com.dnestr.base.utils.WaitUtils;
import com.dnestr.mobile.context.TestContext;
import com.dnestr.mobile.locators.SystemLocators;
import com.dnestr.mobile.locators.TestIdLocators;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.HasOnScreenKeyboard;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.interactions.Pause;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

import static com.dnestr.base.logs.LogStyles.*;

/**
 * Actions inside the app under test that go below the element level: taps and gestures at coordinates or on an
 * element, swiping and scrolling, the keyboard, and reading what the screen shows. Control of the device and of the
 * app from outside it is in {@link DeviceControlUtils}. A method that works on one platform only has that platform in
 * its name and throws {@link UnsupportedOperationException} on the other.
 */
@Slf4j
public class AppInteractionUtils {

    private static final int MAX_REVEAL_DRAGS = 4;
    private static final Duration DRAG_DURATION = Duration.ofMillis(500);

    private AppInteractionUtils() {}

    //region TAPS & GESTURES
    /** Performs a single tap at the given viewport coordinates via a synthetic touch pointer gesture. */
    public static void tapByCoordinates(int x, int y, AppiumDriver driver) {
        log.info("{} {} Tapping coordinates ({}, {})", INFO_SHORT, INFO, x, y);
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");

        Sequence tap = new Sequence(finger, 0)
                .addAction(finger.createPointerMove(Duration.ZERO,
                        PointerInput.Origin.viewport(), x, y))
                .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

        driver.perform(Collections.singletonList(tap));
    }

    /** Taps near the top-center of the screen (10% down from the top) — e.g. for dismissing a top banner or reaching a safe-area control. */
    public static void tapTopSafeArea(AppiumDriver driver) {
        Dimension size = driver.manage().window().getSize();

        int x = size.getWidth() / 2;
        int y = (int)(size.getHeight() * 0.1);

        tapByCoordinates(x, y, driver);
    }

    /** Taps a point unlikely to hit any interactive element (center-horizontal, upper-quarter vertical) — e.g. to dismiss a keyboard/popover by tapping "outside" it. */
    public static void tapNeutralArea(AppiumDriver driver) {
        int w = driver.manage().window().getSize().getWidth();
        int h = driver.manage().window().getSize().getHeight();

        int x = w / 2;
        int y = h / 4;

        tapByCoordinates(x, y, driver);
    }

    /** Taps the center of {@code element} twice, 150 ms apart, as one gesture. */
    public static void doubleTap(WebElement element, AppiumDriver driver) {
        Point center = MobileElementUtils.centerOf(element);
        log.info("{} {} Double-tapping ({}, {})", INFO_SHORT, INFO, center.getX(), center.getY());
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");

        Sequence taps = new Sequence(finger, 0)
                .addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), center.getX(), center.getY()))
                .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(new Pause(finger, Duration.ofMillis(150)))
                .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

        driver.perform(List.of(taps));
    }

    /** Presses and holds the center of {@code element} for {@code hold} before releasing, via a synthetic touch pointer gesture — e.g. to open a context menu or start a drag. */
    public static void longPress(WebElement element, Duration hold, AppiumDriver driver) {
        Point center = MobileElementUtils.centerOf(element);
        log.info("{} {} Long-pressing ({}, {}) for {} ms", INFO_SHORT, INFO, center.getX(), center.getY(), hold.toMillis());
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");

        Sequence press = new Sequence(finger, 0)
                .addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), center.getX(), center.getY()))
                .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(new Pause(finger, hold))
                .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

        driver.perform(List.of(press));
    }

    /**
     * Presses at {@code from}, holds for {@code hold}, moves to {@code to} over {@code move}, then releases — a
     * reorderable list only picks a row up after the hold.
     */
    public static void dragAndDrop(Point from, Point to, Duration hold, Duration move, AppiumDriver driver) {
        log.info("{} {} Dragging ({}, {}) to ({}, {})", INFO_SHORT, INFO, from.getX(), from.getY(), to.getX(), to.getY());
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");

        Sequence drag = new Sequence(finger, 0)
                .addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), from.getX(), from.getY()))
                .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(new Pause(finger, hold))
                .addAction(finger.createPointerMove(move, PointerInput.Origin.viewport(), to.getX(), to.getY()))
                .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

        driver.perform(List.of(drag));
    }

    /**
     * Draws a single continuous zigzag line inside {@code box} without lifting the finger — e.g. to
     * fill a signature pad. The line starts at the upper-left of the box's inner area (1/8 in from
     * the left, 1/3 down), then makes {@code strokes} 150 ms segments, alternating between the 2/3
     * and 1/3 height marks while advancing evenly across the middle 3/4 of the box's width.
     *
     * @throws IllegalArgumentException if {@code strokes} is less than 1
     */
    public static void drawWithFinger(Rectangle box, int strokes, AppiumDriver driver) {
        if (strokes < 1) {
            throw new IllegalArgumentException("strokes must be at least 1, was " + strokes);
        }
        log.info("{} {} Drawing {} strokes in {}", INFO_SHORT, INFO, strokes, box);
        int left = box.getX() + box.getWidth() / 8;
        int step = box.getWidth() * 3 / 4 / strokes;
        int top = box.getY() + box.getHeight() / 3;
        int bottom = box.getY() + box.getHeight() * 2 / 3;
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");

        Sequence stroke = new Sequence(finger, 0)
                .addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), left, top))
                .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));

        for (int i = 1; i <= strokes; i++) {
            stroke.addAction(finger.createPointerMove(Duration.ofMillis(150), PointerInput.Origin.viewport(),
                    left + i * step, i % 2 == 0 ? top : bottom)
            );
        }
        stroke.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(List.of(stroke));
    }
    //endregion

    //region SWIPE & SCROLL
    /**
     * Performs a swipe gesture in {@code direction} (e.g. {@code "up"}/{@code "down"}), platform-
     * appropriately: on iOS, passed through to Appium's {@code mobile: swipe} as-is; on Android,
     * passed as the {@code direction} of a {@code mobile: swipeGesture} over a fixed screen region
     * ({@code left=100, top=600, width=800, height=1000}, {@code percent=0.85}).
     */
    public static void swipe(AppiumDriver driver, String direction) {
        log.info("{} {} Swipe {} gesture", INFO_SHORT, INFO, direction);

        if (TestContext.isIos()) {
            driver.executeScript("mobile: swipe", Map.of("direction", "%s".formatted(direction)));
        } else {
            driver.executeScript("mobile: swipeGesture",
                    Map.of(
                            "left", 100,
                            "top", 600,
                            "width", 800,
                            "height", 1000,
                            "direction", direction,
                            "percent", 0.85
                    ));
        }
    }

    /** {@link #swipe} with direction {@code "up"}. */
    public static void swipeUp(AppiumDriver driver) {
        swipe(driver, "up");
    }

    /** {@link #swipe} with direction {@code "down"} — see {@link #swipe}'s docs for the current Android caveat (this is actually an "up" swipe on Android). */
    public static void swipeDown(AppiumDriver driver) {
        swipe(driver, "down");
    }

    /**
     * Swipes up until {@code done} holds, at most {@code maxSwipes} times, checking before each swipe and after the
     * last; returns whether it held. For a list that loads more rows as its end comes into view, where
     * {@link #scrollIntoView} has nothing to search for yet.
     */
    public static boolean swipeUpUntil(BooleanSupplier done, int maxSwipes, AppiumDriver driver) {
        for (int swipe = 0; swipe < maxSwipes; swipe++) {
            if (done.getAsBoolean()) return true;
            swipeUp(driver);
        }
        return done.getAsBoolean();
    }

    /**
     * Whether a swipe up moved the screen: compares screenshots taken before and after it with
     * {@link MobileValidationUtils#areImagesDifferent} at {@code threshold}. The swipe stays done.
     */
    public static boolean isScrollableBySwipeUp(AppiumDriver driver, double threshold) {
        byte[] before = driver.getScreenshotAs(OutputType.BYTES);
        swipeUp(driver);
        byte[] after = driver.getScreenshotAs(OutputType.BYTES);
        return MobileValidationUtils.areImagesDifferent(before, after, threshold);
    }

    /**
     * Scrolls until the element whose id is {@code id} is on screen and returns it; throws when no scroll reaches it.
     * {@code id} is the Android resource-id (package-qualified in a native app) and the iOS name, where a React
     * Native testID lands on each. Android searches the first scrollable container from its top, so an element above
     * the current position is found too; iOS scrolls straight to it. Pass no direction on iOS: WebDriverAgent would
     * make a single swipe that way and ignore the predicate. iOS drags first: WebDriverAgent's scroll handles table
     * and collection views only, and on a React Native {@code ScrollView} it scrolls back and forth for about a minute
     * before giving up, so it is the fallback for an element above the current position.
     */
    public static WebElement scrollIntoView(String id, AppiumDriver driver) {
        log.info("{} {} Scrolling to '{}'", INFO_SHORT, INFO, id);

        if (TestContext.isIos()) {
            By target = TestIdLocators.iosByTestIdPredicate(id);
            if (!dragUpUntil(() -> isDisplayed(target, driver), MAX_REVEAL_DRAGS, driver)) {
                log.debug("Dragging did not reach '{}', scrolling by predicate", id);
                driver.executeScript("mobile: scroll", Map.of("predicateString", TestIdLocators.iosTestIdPredicate(id)));
            }
            return driver.findElement(target);
        }
        return driver.findElement(TestIdLocators.androidScrollIntoViewByTestId(id));
    }

    /**
     * Drags up from 45% to 15% of the screen height, as a finger scrolls, until {@code done} holds, at most
     * {@code maxDrags} times; returns whether it held. WebDriverAgent's swipe is a flick a React Native scroll view
     * can ignore, which a drag is not. It starts above a bottom footer and keyboard, where a drag scrolls nothing.
     */
    public static boolean dragUpUntil(BooleanSupplier done, int maxDrags, AppiumDriver driver) {
        Dimension size = driver.manage().window().getSize();
        Point from = new Point(size.getWidth() / 2, (int) (size.getHeight() * 0.45));
        Point to = new Point(from.getX(), (int) (size.getHeight() * 0.15));
        for (int drag = 0; drag < maxDrags; drag++) {
            if (done.getAsBoolean()) return true;
            dragAndDrop(from, to, Duration.ZERO, DRAG_DURATION, driver);
        }
        return done.getAsBoolean();
    }

    /** {@code isDisplayed} also counts the clipping of a scroll view, which an element's position alone does not show. */
    private static boolean isDisplayed(By locator, AppiumDriver driver) {
        List<WebElement> found = driver.findElements(locator);
        return !found.isEmpty() && found.getFirst().isDisplayed();
    }

    /**
     * {@link #scrollIntoView(String, AppiumDriver)}, retried every second until {@code timeout} for a list that is
     * still loading or redraws while it is searched; the last failure is rethrown once time is up.
     */
    public static WebElement scrollIntoView(String id, Duration timeout, AppiumDriver driver) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (true) {
            try {
                return scrollIntoView(id, driver);
            } catch (WebDriverException notYet) {
                if (System.nanoTime() >= deadline) throw notYet;
                log.debug("'{}' not reached yet: {}", id, notYet.getMessage());
                WaitUtils.sleepSeconds(1);
            }
        }
    }
    //endregion

    //region KEYBOARD
    /**
     * Hides the on-screen keyboard; does nothing when none is up. On iOS a failed hide presses the keyboard's own Return
     * or Done key, then falls back to a tap near the top of the screen. On Android it is only logged: pressing Back instead leaves the screen whenever the keyboard
     * was not really up, and an emulator with a hardware keyboard reports one shown while drawing none.
     */
    public static void hideKeyboard(AppiumDriver driver) {
        if (driver instanceof HasOnScreenKeyboard keyboard && !keyboard.isKeyboardShown()) return;
        try {
            if (TestContext.isAndroid()) {
                ((AndroidDriver) driver).hideKeyboard();
            } else {
                ((IOSDriver) driver).hideKeyboard();
            }
        } catch (Exception e) {
            if (TestContext.isAndroid()) {
                log.warn("⚠ Failed to hide keyboard, leaving it: {}", e.getMessage());
                return;
            }
            log.warn("⚠ Failed to hide keyboard, pressing its return key instead: {}", e.getMessage());
            if (!pressIosKeyboardDismissKey(driver)) tapNearTopQuietly(driver);
        }
    }

    /** WebDriverAgent cannot dismiss a keyboard whose only way out is its return key, such as React Native's default one. */
    private static boolean pressIosKeyboardDismissKey(AppiumDriver driver) {
        try {
            List<WebElement> keys = driver.findElements(SystemLocators.iosKeyboardDismissKey());
            if (keys.isEmpty()) return false;
            keys.getFirst().click();
            return !((HasOnScreenKeyboard) driver).isKeyboardShown();
        } catch (Exception e) {
            log.warn("⚠ Failed to press the keyboard's return key, tapping near the top instead: {}", e.getMessage());
            return false;
        }
    }

    private static void tapNearTopQuietly(AppiumDriver driver) {
        try {
            Dimension size = driver.manage().window().getSize();
            tapByCoordinates(size.getWidth() / 2, size.getHeight() / 10, driver);
        } catch (Exception e) {
            log.warn("⚠ Failed to hide keyboard by tapping: {}", e.getMessage());
        }
    }

    /** Taps "Done" on the iOS keyboard's toolbar, which a number pad needs to close. */
    public static void tapIosKeyboardDoneButton(AppiumDriver driver) {
        PlatformGuards.requireIos("the keyboard's Done button");
        driver.findElement(SystemLocators.iosKeyboardDoneButton()).click();
    }

    /** Types {@code text} into whichever element has focus, for an input the UI tree leaves out (e.g. a 1x1 code field). */
    public static void typeIntoFocusedElement(String text, AppiumDriver driver) {
        new Actions(driver).sendKeys(text).perform();
    }
    //endregion

    //region SCREEN CONTENT
    /**
     * Every text in the current page source, in document order and without repeats: Android's {@code text}
     * attributes, iOS's {@code label} and {@code value}. Covers screens of other apps (a dialer, a browser) too.
     */
    public static List<String> getPageSourceTexts(AppiumDriver driver) {
        List<String> attributes = TestContext.isIos() ? List.of("label", "value") : List.of("text");
        Set<String> texts = new LinkedHashSet<>();
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            var document = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(driver.getPageSource().getBytes(StandardCharsets.UTF_8)));
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
