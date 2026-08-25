package com.dnestr.core.tools.ado;

public record AdoSyncConfig(
        String project,
        int planId,
        int parentSuiteId
) {}
