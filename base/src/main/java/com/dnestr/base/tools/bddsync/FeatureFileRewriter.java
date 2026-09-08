package com.dnestr.base.tools.bddsync;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class FeatureFileRewriter {

    public record Update(
            int anchorLineIndex,
            boolean insertNewLine,
            String originalTagLine,
            String tagToAdd,
            Set<String> tagsToRemove
    ) {}

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
