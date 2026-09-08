package com.dnestr.base.tools.bddsync;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public final class FeatureFileScanner {

    public record ScanResult(List<SyncCandidate> candidates, List<String> warnings) {
    }

    public ScanResult scan(Path featuresRoot) {
        List<SyncCandidate> candidates = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        try (Stream<Path> files = Files.walk(featuresRoot)) {
            files.filter(p -> p.toString().endsWith(".feature"))
                    .sorted()
                    .forEach(file -> scanFile(file, candidates, warnings));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to walk features directory: " + featuresRoot, e);
        }

        return new ScanResult(candidates, warnings);
    }

    private void scanFile(Path file, List<SyncCandidate> candidates, List<String> warnings) {
        List<String> lines;
        try {
            lines = Files.readAllLines(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read feature file: " + file, e);
        }

        List<String> featureTags = List.of();
        String featureTitle = null;
        List<String> pendingTagLines = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String trimmed = line.trim();

            if (isTagLine(trimmed)) {
                pendingTagLines.add(line);
                continue;
            }

            if (trimmed.startsWith("Feature:")) {
                featureTags = tagsOf(pendingTagLines);
                featureTitle = trimmed.substring("Feature:".length()).trim();
                pendingTagLines.clear();
                continue;
            }

            if (trimmed.startsWith("Scenario Outline:") || trimmed.startsWith("Scenario:")) {
                String prefix = trimmed.startsWith("Scenario Outline:") ? "Scenario Outline:" : "Scenario:";

                List<String> ownTags = tagsOf(pendingTagLines);
                boolean hasSingleOwnTagLine = pendingTagLines.size() == 1;
                boolean hasNoOwnTagLine = pendingTagLines.isEmpty();
                int anchorLineIndex = hasSingleOwnTagLine ? i - 1 : i;
                String originalTagLine = hasSingleOwnTagLine ? pendingTagLines.getFirst() : null;
                pendingTagLines.clear();

                List<String> combined = new ArrayList<>(featureTags);
                combined.addAll(ownTags);

                String title = trimmed.substring(prefix.length()).trim();

                int bodyStart = i;
                int j = i + 1;
                while (j < lines.size()) {
                    String next = lines.get(j).trim();
                    if (isTagLine(next) || next.startsWith("Scenario:")
                            || next.startsWith("Scenario Outline:") || next.startsWith("Feature:")) {
                        break;
                    }
                    j++;
                }
                int bodyEnd = j;
                while (bodyEnd > bodyStart + 1 && lines.get(bodyEnd - 1).trim().isEmpty()) {
                    bodyEnd--;
                }
                String body = String.join("\n", lines.subList(bodyStart, bodyEnd));
                i = j - 1;

                if (!hasSingleOwnTagLine && !hasNoOwnTagLine) {
                    warnings.add(
                            file + ": "
                                    + prefix.substring(0, prefix.length() - 1)
                                    + " '" + title
                                    + "' has tags spanning more than one line - skipping "
                                    + "(rewrite isn't supported for that shape)."
                    );
                    continue;
                }

                candidates.add(new SyncCandidate(
                        file,
                        featureTitle,
                        anchorLineIndex,
                        hasNoOwnTagLine,
                        originalTagLine,
                        title,
                        body,
                        List.copyOf(combined)
                ));
            }
        }
    }

    private boolean isTagLine(String trimmed) {
        if (trimmed.isEmpty()) {
            return false;
        }
        return Arrays.stream(trimmed.split("\\s+")).allMatch(token -> token.startsWith("@"));
    }

    private List<String> tagsOf(List<String> tagLines) {
        List<String> tags = new ArrayList<>();
        for (String line : tagLines) {
            for (String token : line.trim().split("\\s+")) {
                if (token.startsWith("@")) {
                    tags.add(token);
                }
            }
        }
        return tags;
    }
}
