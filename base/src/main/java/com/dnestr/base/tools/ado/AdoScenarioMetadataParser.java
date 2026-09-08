package com.dnestr.base.tools.ado;

import com.dnestr.base.tools.bddsync.SyncCandidate;

import java.util.List;
import java.util.Optional;

public final class AdoScenarioMetadataParser {

    public AdoScenarioMetadata parse(SyncCandidate candidate) {
        List<String> sprintTags = candidate.tagsWithPrefix("@S:");

        if (sprintTags.size() != 1) {
            throw new IllegalStateException(
                    "'%s' must contain exactly one @S: tag, found: %s"
                            .formatted(candidate.scenarioTitle(), sprintTags)
            );
        }

        List<String> tcTags = candidate.tagsWithPrefix("@tc:");

        if (tcTags.size() > 1) {
            throw new IllegalStateException(
                    "'%s' contains multiple @tc: tags: %s"
                            .formatted(candidate.scenarioTitle(), tcTags)
            );
        }

        String sprint = valueOf(sprintTags.getFirst(), "@S:");

        Optional<Long> testCaseId = tcTags.isEmpty()
                ? Optional.empty()
                : Optional.of(parseTestCaseId(valueOf(tcTags.getFirst(), "@tc:")));

        List<Long> pbiIds = candidate.tagsWithPrefix("@us:").stream()
                .map(tag -> valueOf(tag, "@us:"))
                .map(this::parsePbiId)
                .distinct()
                .toList();

        return new AdoScenarioMetadata(
                sprint,
                testCaseId,
                pbiIds
        );
    }

    private String valueOf(String tag, String prefix) {
        String value = tag.substring(prefix.length());

        if (value.isBlank()) {
            throw new IllegalStateException(
                    "Tag '%s' has no value".formatted(tag)
            );
        }
        return value;
    }

    private long parsePbiId(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                    "Invalid @us: work item ID: '" + value + "'",
                    e
            );
        }
    }

    private long parseTestCaseId(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                    "Invalid @tc: test case ID: '" + value + "'",
                    e
            );
        }
    }
}
