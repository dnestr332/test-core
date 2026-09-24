package com.dnestr.base.tools.bddsync;

import java.nio.file.Path;
import java.util.List;

/**
 * One {@code Scenario}/{@code Scenario Outline} found by {@link FeatureFileScanner}, along with
 * everything a {@link SyncProvider} needs to decide whether/how to sync it and everything
 * {@link FeatureFileRewriter} needs to write a tag change back into the {@code .feature} file
 * afterward.
 *
 * @param file              the {@code .feature} file this scenario was found in
 * @param featureTitle      the enclosing {@code Feature:}'s title
 * @param anchorLineIndex   0-based line index to rewrite: the scenario's own single tag line if it
 *                          has exactly one ({@code insertNewLine} is {@code false}), otherwise the
 *                          {@code Scenario:}/{@code Scenario Outline:} line itself, where a new tag
 *                          line will be inserted just above ({@code insertNewLine} is {@code true})
 * @param insertNewLine     whether a new tag line must be inserted rather than an existing one rewritten
 * @param originalTagLine   the scenario's existing tag line as written in the file, or {@code null}
 *                          if it has none yet
 * @param scenarioTitle     the scenario's title
 * @param gherkinBody        the scenario's raw Gherkin text (its own line plus every step line)
 * @param tags              every tag in effect for this scenario — inherited {@code Feature:}-level
 *                          tags plus the scenario's own, in file order
 */
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

    /** Whether {@code tag} (case-insensitive) is among this scenario's effective tags. */
    public boolean hasTag(String tag) {
        return tags.stream().anyMatch(tag::equalsIgnoreCase);
    }

    /** Every tag (case-insensitively) starting with {@code prefix}, e.g. {@code tagsWithPrefix("@tc:")} for {@code ["@tc:123"]}. */
    public List<String> tagsWithPrefix(String prefix) {
        return tags.stream()
                .filter(tag -> tag.regionMatches(
                        true, 0, prefix, 0, prefix.length()))
                .toList();
    }
}
