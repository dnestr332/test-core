package com.dnestr.base.tools.ado;

/**
 * Fixed ADO target for a sync run — which project, test plan, and parent suite new sprint suites
 * get created under.
 *
 * @param project        the ADO project name (used to build every REST API URL)
 * @param planId         the ADO test plan ID that scenarios sync into
 * @param parentSuiteId  the suite ID under which a new per-sprint suite is created, when a sprint
 *                        named by a scenario's {@code @S:} tag doesn't already have one
 */
public record AdoSyncConfig(
        String project,
        int planId,
        int parentSuiteId
) {}
