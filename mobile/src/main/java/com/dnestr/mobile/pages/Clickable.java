package com.dnestr.mobile.pages;

/**
 * Marker implemented by a screen's element enum (one constant per interactive element), used with
 * {@link BaseScreen#resolve} to look up that element's {@code By} locator and with
 * {@code BaseActionFlow#dynamicTap} to guard against tapping an element the screen itself marks as
 * non-interactive (e.g. a disabled/decorative item) before ever reaching the driver.
 */
public interface Clickable {

    /** Whether this element is expected to be interactable — checked by {@code BaseActionFlow#dynamicTap} before it attempts a tap. */
    boolean isClickable();
}
