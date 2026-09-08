package com.dnestr.mobile.flows;

import com.dnestr.mobile.pages.BaseScreen;
import com.dnestr.mobile.pages.Clickable;
import com.dnestr.mobile.actions.ElementActions;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class BaseActionFlow {

    protected final ElementActions elementActions;

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

    protected void dynamicType(BaseScreen screen, Clickable item, String value) {
        elementActions.type(screen.resolve(item), value);
    }

    protected String dynamicGetText(BaseScreen screen, Clickable item) {
        return elementActions.text(screen.resolve(item));
    }
}
