package com.dnestr.base.tools.bddsync;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates one end-to-end sync run: scan every {@code .feature} file under a root directory,
 * let a {@link SyncProvider} claim and sync the scenarios it owns, then rewrite each affected
 * file's tags to reflect the result. A single candidate's sync failure is caught and recorded
 * rather than aborting the run, so one bad scenario doesn't block every other one from syncing.
 */
public final class BddSyncEngine {

    private final FeatureFileScanner scanner;
    private final FeatureFileRewriter rewriter;

    public BddSyncEngine() {
        this.scanner = new FeatureFileScanner();
        this.rewriter = new FeatureFileRewriter();
    }

    /**
     * Scans {@code featuresRoot}, filters to scenarios {@code provider} claims via
     * {@link SyncProvider#shouldSync}, and — unless {@code dryRun} — calls
     * {@link SyncProvider#sync} on each and rewrites every affected file's tags via
     * {@link FeatureFileRewriter} in one pass per file. In dry-run mode, scanning and filtering
     * still happen (so the report reflects what would be synced) but no external calls are made
     * and no files are touched.
     */
    public SyncReport sync(Path featuresRoot, SyncProvider provider, boolean dryRun) {
        FeatureFileScanner.ScanResult scanResult = scanner.scan(featuresRoot);

        List<SyncCandidate> candidates = scanResult.candidates().stream()
                .filter(provider::shouldSync)
                .toList();

        if (dryRun) {
            return SyncReport.dryRun(candidates, scanResult.warnings());
        }

        Map<Path, List<FeatureFileRewriter.Update>> updates = new HashMap<>();
        List<SyncReport.Failure> failures = new ArrayList<>();

        for (SyncCandidate candidate : candidates) {
            try {
                SyncResult result = provider.sync(candidate);

                updates.computeIfAbsent(candidate.file(), ignored -> new ArrayList<>())
                        .add(new FeatureFileRewriter.Update(
                                candidate.anchorLineIndex(),
                                candidate.insertNewLine(),
                                candidate.originalTagLine(),
                                result.tagToAdd(),
                                result.tagsToRemove()
                        ));

            } catch (Exception e) {
                failures.add(new SyncReport.Failure(candidate, e));
            }
        }

        updates.forEach(rewriter::applyUpdates);

        int synced = updates.values().stream()
                .mapToInt(List::size)
                .sum();

        return new SyncReport(
                candidates.size(),
                synced,
                List.copyOf(failures),
                List.copyOf(scanResult.warnings()),
                false
        );
    }
}
