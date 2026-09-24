package com.dnestr.base.states;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A binary on/off state paired with its numeric (1/0) representation, e.g. for a value sent to an
 * API or compared against a raw integer read from device/UI state. Unused within {@code test-core}
 * itself (no reference to it elsewhere in this module) — available for a consuming app to use where
 * a plain {@code ON}/{@code OFF} concept, rather than {@link ToggleState}'s checked/unchecked
 * framing, fits the scenario better.
 */
@Getter
@RequiredArgsConstructor
public enum SwitchState {

    ON(1),
    OFF(0);

    /** Numeric representation of this state: 1 for {@code ON}, 0 for {@code OFF}. */
    private final int value;
}
