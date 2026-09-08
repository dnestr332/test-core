package com.dnestr.mobile.pages;

import com.dnestr.mobile.actions.ElementActions;
import lombok.RequiredArgsConstructor;
import org.openqa.selenium.By;

@RequiredArgsConstructor
public abstract class BaseScreen implements Screen, Closable {

    protected final ElementActions elementActions;

    public abstract By resolve(Clickable item);

    protected <T extends Clickable> T cast(Clickable item, Class<T> type) {
        if (!type.isInstance(item)) {
            throw new IllegalStateException(
                    "Expected %s but got %s (%s)"
                            .formatted(type.getSimpleName(), item, item.getClass().getSimpleName())
            );
        }
        return type.cast(item);
    }

    protected By title() {
        throw new UnsupportedOperationException(
                "Title is not defined for %s".formatted(getClass().getSimpleName())
        );
    }

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
