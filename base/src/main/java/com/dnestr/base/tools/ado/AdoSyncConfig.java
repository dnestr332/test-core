package com.dnestr.base.tools.ado;

public record AdoSyncConfig(
        String project,
        int planId,
        int parentSuiteId
) {}
