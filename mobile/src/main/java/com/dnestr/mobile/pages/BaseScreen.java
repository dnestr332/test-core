package com.dnestr.mobile.pages;

import com.dnestr.mobile.actions.ElementActions;
import lombok.RequiredArgsConstructor;
import org.openqa.selenium.By;

/**
 * Base for a single screen's page object (mobile's counterpart to the web module's
 * {@code BasePage}). Holds the shared {@link ElementActions} instance (passed in by the subclass
 * constructor via {@link RequiredArgsConstructor} — used by composition/injection, not extended by
 * {@code ElementActions} itself), and provides the {@link Clickable} → {@code By} lookup plus the
 * default {@link Screen}/{@link Closable} implementations. Subclasses add screen-specific locator
 * methods and behavior on top.
 */
@RequiredArgsConstructor
public abstract class BaseScreen implements Screen, Closable {

    protected final ElementActions elementActions;

    /** Resolves a {@link Clickable} element constant to its {@code By} locator; implemented per screen. */
    public abstract By resolve(Clickable item);

    /**
     * Narrows {@code item} to {@code type}, for a flow that only makes sense for a specific screen's
     * element enum (e.g. dispatching on which concrete {@link Clickable} implementation was passed).
     *
     * @throws IllegalStateException if {@code item} isn't an instance of {@code type}
     */
    protected <T extends Clickable> T cast(Clickable item, Class<T> type) {
        if (!type.isInstance(item)) {
            throw new IllegalStateException(
                    "Expected %s but got %s (%s)"
                            .formatted(type.getSimpleName(), item, item.getClass().getSimpleName())
            );
        }
        return type.cast(item);
    }

    /**
     * The screen's title element locator, backing the default {@link Screen} methods below.
     * Subclasses that have a title should override this; the default throws, so a screen that never
     * overrides it but calls {@link #isTitleVisible()}/{@link #getTitleText()} fails loudly rather
     * than silently checking the wrong (or no) element.
     */
    protected By title() {
        throw new UnsupportedOperationException(
                "Title is not defined for %s".formatted(getClass().getSimpleName())
        );
    }

    /**
     * The screen's close button locator, backing the default {@link Closable} implementation below.
     * Subclasses that are closable should override this; the default throws for the same reason as
     * {@link #title()}.
     */
    protected By closeButton() {
        throw new UnsupportedOperationException(
                "Close button is not defined for %s".formatted(getClass().getSimpleName())
        );
    }

    @Override
    public boolean isTitleVisible() {
        return elementActions.isVisible(title());
    }

    @Override
    public String getTitleText() {
        return elementActions.text(title());
    }

    @Override
    public void close() {
        elementActions.click(closeButton());
    }
}
