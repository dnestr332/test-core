package com.dnestr.mobile.flows;

import com.dnestr.mobile.actions.ElementActions;
import com.dnestr.mobile.screens.ScreenElement;
import com.dnestr.mobile.utils.ValidationUtils;
import lombok.RequiredArgsConstructor;

/**
 * The shared tap, type and text on a screen element, which reads its own locator. An app's flow extends it to add the
 * sequences a scenario needs, or to override a tap or type that needs more than a click.
 */
@RequiredArgsConstructor
public abstract class BaseActionFlow {

    private static final int CHECK_ATTEMPTS = 3;

    protected final ElementActions elementActions;

    public void tap(ScreenElement element) {
        elementActions.click(element.getLocator());
    }

    /** Types {@code value} into {@code element}, replacing its content. */
    public void type(ScreenElement element, String value) {
        elementActions.type(element.getLocator(), value);
    }

    public String text(ScreenElement element) {
        return elementActions.text(element.getLocator());
    }

    /**
     * Ticks or unticks a box. It can open ticked with a consent given earlier, and on iOS a tap right after the screen
     * opens can be dropped, so it is tapped until it reads as wanted, at most three times.
     */
    public void setChecked(ScreenElement box, boolean checked) {
        for (int tap = 0; tap < CHECK_ATTEMPTS && ValidationUtils.isChecked(elementActions.find(box.getLocator())) != checked; tap++) {
            tap(box);
        }
    }
}
