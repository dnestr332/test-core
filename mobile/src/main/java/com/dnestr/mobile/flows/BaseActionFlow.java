package com.dnestr.mobile.flows;

import com.dnestr.mobile.pages.BaseScreen;
import com.dnestr.mobile.pages.Clickable;
import com.dnestr.mobile.actions.ElementActions;
import lombok.RequiredArgsConstructor;

/**
 * Base for flow classes that act on a {@link BaseScreen} via a {@link Clickable} element reference
 * rather than a resolved locator — the web module's counterpart resolves a page enum + element
 * through a {@code BasePageResolver}; here the caller already has the target {@link BaseScreen} and
 * this class only adds the {@link Clickable} → {@code By} indirection (via
 * {@link BaseScreen#resolve}) plus the pre-tap clickability guard.
 */
@RequiredArgsConstructor
public abstract class BaseActionFlow {

    protected final ElementActions elementActions;

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
        var locator = screen.resolve(item);
        elementActions.click(locator);
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
