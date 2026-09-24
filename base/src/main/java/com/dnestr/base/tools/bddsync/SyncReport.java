package com.dnestr.base.tools.bddsync;

import java.util.List;

/**
 * Summary of one {@link BddSyncEngine#sync} run.
 *
 * @param candidates how many scenarios the provider claimed via {@link SyncProvider#shouldSync}
 * @param synced     how many feature-file tag updates were actually written (0 for a dry run,
 *                   possibly less than {@code candidates} if some failed)
 * @param failures   one entry per candidate whose {@link SyncProvider#sync} call threw — a single
 *                   failure doesn't abort the run; every other candidate is still attempted
 * @param warnings   non-fatal issues from {@link FeatureFileScanner} (e.g. a scenario whose tags
 *                   span multiple lines, which isn't rewritable, so it was skipped entirely)
 * @param dryRun     whether this was a dry run — providers' {@code shouldSync} ran but {@code sync} never did
 */
public record SyncReport(
        int candidates,
        int synced,
        List<Failure> failures,
        List<String> warnings,
        boolean dryRun
) {

    /** Builds a dry-run report: candidate/warning counts as scanned, zero synced, no failures (nothing was attempted). */
    public static SyncReport dryRun(List<SyncCandidate> candidates, List<String> warnings) {
        return new SyncReport(
                candidates.size(),
                0,
                List.of(),
                List.copyOf(warnings),
                true
        );
    }

    /** One candidate whose sync attempt threw, paired with the exception it threw. */
    public record Failure(
            SyncCandidate candidate,
            Exception exception
    ) {}
}
