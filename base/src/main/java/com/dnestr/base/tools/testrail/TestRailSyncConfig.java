package com.dnestr.base.tools.testrail;

/**
 * Fixed TestRail target for a sync run.
 *
 * @param suiteId          the TestRail suite that scenarios sync into (also used to resolve the project)
 * @param parentSectionId  the section under which a new per-feature-file subsection is created,
 *                         when a feature's title doesn't already have one (see
 *                         {@link TestRailCaseSyncClient#resolveOrCreateSubsection})
 */
public record TestRailSyncConfig(
        int suiteId,
        int parentSectionId
) {
}
