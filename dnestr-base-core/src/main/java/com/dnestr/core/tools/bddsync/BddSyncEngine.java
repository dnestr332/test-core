package com.dnestr.core.tools.bddsync;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BddSyncEngine {

    private final FeatureFileScanner scanner;
    private final FeatureFileRewriter rewriter;

    public BddSyncEngine() {
        this.scanner = new FeatureFileScanner();
        this.rewriter = new FeatureFileRewriter();
    }

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
