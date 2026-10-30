package com.dnestr.base.states;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A binary on/off state paired with its numeric (1/0) representation, e.g. for a device setting written as an
 * integer ({@code DeviceControlUtils.setAndroidHeadsUpNotifications}) or a raw value read back from the UI. Use it
 * where a plain {@code ON}/{@code OFF} fits better than {@link ToggleState}'s checked/unchecked.
 */
@Getter
@RequiredArgsConstructor
public enum SwitchState {

    ON(1),
    OFF(0);

    /** Numeric representation of this state: 1 for {@code ON}, 0 for {@code OFF}. */
    private final int value;
}
