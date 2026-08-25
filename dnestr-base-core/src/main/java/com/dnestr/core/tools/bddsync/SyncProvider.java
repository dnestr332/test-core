package com.dnestr.core.tools.bddsync;

public interface SyncProvider {

    boolean shouldSync(SyncCandidate candidate);

    SyncResult sync(SyncCandidate candidate);
}
