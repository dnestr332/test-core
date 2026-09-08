package com.dnestr.base.tools.bddsync;

import java.nio.file.Path;
import java.util.List;

public record SyncCandidate(
        Path file,
        String featureTitle,
        int anchorLineIndex,
        boolean insertNewLine,
        String originalTagLine,
        String scenarioTitle,
        String gherkinBody,
        List<String> tags
) {

    public boolean hasTag(String tag) {
        return tags.stream().anyMatch(tag::equalsIgnoreCase);
    }

    public List<String> tagsWithPrefix(String prefix) {
        return tags.stream()
                .filter(tag -> tag.regionMatches(
                        true, 0, prefix, 0, prefix.length()))
                .toList();
    }
}
