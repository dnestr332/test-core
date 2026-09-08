package com.dnestr.base.states;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SwitchState {

    ON(1),
    OFF(0);

    private final int value;
}
