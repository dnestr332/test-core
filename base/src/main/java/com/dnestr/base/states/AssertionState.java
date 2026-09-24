package com.dnestr.base.states;

/**
 * Chooses failure semantics for a {@code verify*} call in {@code BaseAssertionFlow}: {@code STRICTLY}
 * fails the test immediately (routes to {@code Hardly}), {@code SOFTLY} accumulates the failure for
 * a later batched report (routes to {@code Softly}).
 */
public enum AssertionState {

    STRICTLY,
    SOFTLY
}
