package com.dnestr.base.tools.bddsync;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Rewrites tag lines into a {@code .feature} file in place, given the line positions
 * {@link FeatureFileScanner} recorded for each scenario and the tag changes a {@link SyncProvider}
 * decided on (as {@link SyncResult}, carried here via {@link Update}).
 */
public final class FeatureFileRewriter {

    /**
     * One scenario's tag change to apply, combining its {@link SyncCandidate} position fields with
     * a {@link SyncResult}'s tag delta.
     *
     * @param anchorLineIndex the line index to rewrite ({@link SyncCandidate#anchorLineIndex})
     * @param insertNewLine   whether to insert a new tag line rather than rewrite an existing one
     *                        ({@link SyncCandidate#insertNewLine})
     * @param originalTagLine the scenario's existing tag line, used as the base when rewriting
     *                        (ignored when {@code insertNewLine} is {@code true})
     * @param tagToAdd        the tag to add ({@link SyncResult#tagToAdd})
     * @param tagsToRemove    tags to drop from the existing line, if any ({@link SyncResult#tagsToRemove})
     */
    public record Update(
            int anchorLineIndex,
            boolean insertNewLine,
            String originalTagLine,
            String tagToAdd,
            Set<String> tagsToRemove
    ) {}

    /**
     * Applies every {@code update} to {@code file} in one read-modify-write pass: line replacements
     * (existing tag lines) are applied first, then new-line insertions are applied from the bottom
     * of the file upward (highest {@code anchorLineIndex} first) so that inserting a line doesn't
     * shift the still-pending anchor indices of the insertions above it.
     */
    public void applyUpdates(Path file, List<Update> updates) {
        List<String> lines;

        try {
            lines = new ArrayList<>(Files.readAllLines(file));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read feature file for rewrite: " + file, e);
        }

        List<Update> replacements = updates.stream()
                .filter(update -> !update.insertNewLine())
                .toList();

        List<Update> insertions = updates.stream()
                .filter(Update::insertNewLine)
                .toList();

        for (Update update : replacements) {
            lines.set(
                    update.anchorLineIndex(),
                    updateTagLine(update)
            );
        }

        for (Update update : insertions.stream()
                .sorted(Comparator.comparingInt(Update::anchorLineIndex).reversed())
                .toList()) {

            String scenarioLine = lines.get(update.anchorLineIndex());

            String indentation = scenarioLine.substring(
                    0,
                    scenarioLine.length() - scenarioLine.stripLeading().length()
            );

            lines.add(
                    update.anchorLineIndex(),
                    indentation + update.tagToAdd()
            );
        }

        try {
            Files.write(file, lines);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write feature file: " + file, e);
        }
    }

    private String updateTagLine(Update update) {
        String originalLine = update.originalTagLine();

        String leading = originalLine.substring(
                0,
                originalLine.length() - originalLine.stripLeading().length()
        );

        List<String> tags = new ArrayList<>();

        for (String tag : originalLine.trim().split("\\s+")) {
            if (!shouldRemove(tag, update.tagsToRemove())) {
                tags.add(tag);
            }
        }

        tags.add(update.tagToAdd());

        return leading + String.join(" ", tags);
    }

    private boolean shouldRemove(String tag, Set<String> tagsToRemove) {
        return tagsToRemove.stream()
                .anyMatch(tag::equalsIgnoreCase);
    }
}
