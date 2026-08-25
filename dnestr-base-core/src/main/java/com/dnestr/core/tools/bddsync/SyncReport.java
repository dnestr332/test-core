package com.dnestr.core.tools.bddsync;

import java.util.List;

public record SyncReport(
        int candidates,
        int synced,
        List<Failure> failures,
        List<String> warnings,
        boolean dryRun
) {

    public static SyncReport dryRun(List<SyncCandidate> candidates, List<String> warnings) {
        return new SyncReport(
                candidates.size(),
                0,
                List.of(),
                List.copyOf(warnings),
                true
        );
    }

    public record Failure(
            SyncCandidate candidate,
            Exception exception
    ) {}
}
