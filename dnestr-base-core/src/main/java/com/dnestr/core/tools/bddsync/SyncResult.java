package com.dnestr.core.tools.bddsync;

import java.util.Set;

public record SyncResult(
        String tagToAdd,
        Set<String> tagsToRemove
) {

    public static SyncResult add(String tag) {
        return new SyncResult(tag, Set.of());
    }

    public static SyncResult replace(String tag, String tagToRemove) {
        return new SyncResult(tag, Set.of(tagToRemove));
    }
}
