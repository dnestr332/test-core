package com.dnestr.base.tools.bddsync;

/**
 * Pluggable per-test-management-system strategy driving {@link BddSyncEngine}: decides which
 * scenarios it owns (typically by a tag convention, e.g. requiring a specific prefix tag) and
 * performs the actual create-or-update call against that system. Implemented once per external
 * system — see {@code AdoSyncProvider}, {@code TestRailSyncProvider} — so multiple providers can
 * run over the same feature files without interfering, as long as their tag conventions don't overlap.
 */
public interface SyncProvider {

    /** Whether this provider owns {@code candidate} and should sync it (typically based on which tags it carries). */
    boolean shouldSync(SyncCandidate candidate);

    /**
     * Creates or updates {@code candidate} in the external system, returning the tag change to
     * make in the feature file to reflect the result (e.g. recording a newly created test case's ID).
     */
    SyncResult sync(SyncCandidate candidate);
}
