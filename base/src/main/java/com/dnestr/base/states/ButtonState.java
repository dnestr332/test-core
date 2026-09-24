package com.dnestr.base.states;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Expected enabled/disabled state of a button-like control, used with {@code BaseAssertionFlow#verifyButtonState}. */
@Getter
@RequiredArgsConstructor
public enum ButtonState {

    ENABLED(true),
    DISABLED(false);

    /** Whether this state corresponds to the control being enabled. */
    private final boolean enabled;
}
