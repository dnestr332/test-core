package com.dnestr.base.tools.bddsync;

import java.util.Set;

/**
 * The tag-line edit a {@link SyncProvider} wants applied to a scenario's feature file after a
 * successful sync, applied by {@link FeatureFileRewriter}: add {@code tagToAdd}, and drop any tag
 * in {@code tagsToRemove} (case-insensitive) if present.
 */
public record SyncResult(
        String tagToAdd,
        Set<String> tagsToRemove
) {

    /** A result that only adds {@code tag} — use after creating a brand-new external record with no prior tag to replace. */
    public static SyncResult add(String tag) {
        return new SyncResult(tag, Set.of());
    }

    /** A result that adds {@code tag} and drops {@code tagToRemove} — use after updating an already-linked external record. */
    public static SyncResult replace(String tag, String tagToRemove) {
        return new SyncResult(tag, Set.of(tagToRemove));
    }
}
