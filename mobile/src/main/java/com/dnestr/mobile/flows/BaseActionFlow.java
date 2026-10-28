package com.dnestr.mobile.flows;

import com.dnestr.mobile.actions.ElementActions;
import com.dnestr.mobile.screens.BaseScreen;
import com.dnestr.mobile.screens.BaseScreenResolver;
import com.dnestr.mobile.screens.Clickable;
import com.dnestr.mobile.screens.AppScreen;

/**
 * Base for flow classes that act on screens without callers resolving a {@link BaseScreen} themselves — mobile's
 * counterpart to the web module's {@code BaseActionFlow}. The public methods take a {@link AppScreen} constant,
 * looked up through the injected {@link BaseScreenResolver}; the {@code dynamic*} methods take a screen already in hand.
 * Subclasses add the multi-step sequences a scenario needs on top.
 */
public abstract class BaseActionFlow {

    protected final ElementActions elementActions;
    protected final BaseScreenResolver screenResolver;

    protected BaseActionFlow(ElementActions elementActions, BaseScreenResolver screenResolver) {
        this.elementActions = elementActions;
        this.screenResolver = screenResolver;
    }

    /** The {@link BaseScreen} that draws {@code screen}. */
    protected BaseScreen resolve(AppScreen screen) {
        return screenResolver.resolveScreen(screen);
    }

    /** Clicks {@code item} on {@code screen}; see {@link #dynamicTap}. */
    public void tap(AppScreen screen, Clickable item) {
        dynamicTap(resolve(screen), item);
    }

    /** Types {@code value} into {@code item} on {@code screen}, replacing its content. */
    public void type(AppScreen screen, Clickable item, String value) {
        dynamicType(resolve(screen), item, value);
    }

    /** The text of {@code item} on {@code screen}. */
    public String text(AppScreen screen, Clickable item) {
        return dynamicGetText(resolve(screen), item);
    }

    /**
     * Resolves {@code item} on {@code screen} and clicks it.
     *
     * @throws IllegalStateException if {@code item.isClickable()} is {@code false} — checked before
     *                                resolving or touching the driver at all
     */
    protected void dynamicTap(BaseScreen screen, Clickable item) {
        if (!item.isClickable()) {
            throw new IllegalStateException(
                    "Item %s is not clickable on screen %s"
                            .formatted(item, screen.getClass().getSimpleName())
            );
        }
        elementActions.click(screen.resolve(item));
    }

    /** Resolves {@code item} on {@code screen} and types {@code value} into it. */
    protected void dynamicType(BaseScreen screen, Clickable item, String value) {
        elementActions.type(screen.resolve(item), value);
    }

    /** Resolves {@code item} on {@code screen} and returns its text. */
    protected String dynamicGetText(BaseScreen screen, Clickable item) {
        return elementActions.text(screen.resolve(item));
    }
}
