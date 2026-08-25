package com.dnestr.core.tools.testrail;

import com.dnestr.core.tools.bddsync.SyncCandidate;
import com.dnestr.core.tools.bddsync.SyncProvider;
import com.dnestr.core.tools.bddsync.SyncResult;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public final class TestRailSyncProvider implements SyncProvider {

    private final TestRailCaseSyncClient client;
    private final TestRailSyncConfig config;

    private final int projectId;
    private final int templateId;
    private final TestRailCaseSyncClient.CaseField gherkinField;
    private final Optional<TestRailCaseSyncClient.CaseField> automationField;

    private final Map<Path, Integer> subsectionIdByFile = new HashMap<>();

    public TestRailSyncProvider(
            TestRailCaseSyncClient client,
            TestRailSyncConfig config
    ) {
        this.client = client;
        this.config = config;

        this.projectId = client.resolveProjectId(config.suiteId());
        this.templateId = client.resolveGherkinTemplateId(projectId);

        this.gherkinField =
                client.resolveRequiredField("testrail_bdd_scenario");

        this.automationField =
                client.resolveField("is_automated", "is automated")
                        .or(() -> client.resolveField("automat"));
    }

    @Override
    public boolean shouldSync(SyncCandidate candidate) {
        return candidate.hasTag("@sync")
                && candidate.tags().stream()
                .noneMatch(tag -> tag.matches("(?i)@C\\d+"));
    }

    @Override
    public SyncResult sync(SyncCandidate candidate) {

        int sectionId = subsectionIdByFile.computeIfAbsent(
                candidate.file(),
                ignored -> client.resolveOrCreateSubsection(
                        projectId,
                        config.suiteId(),
                        config.parentSectionId(),
                        candidate.featureTitle()
                )
        );

        Map<String, Object> customFields = new HashMap<>();

        customFields.put(
                gherkinField.systemName(),
                List.of(Map.of(
                        "content",
                        candidate.gherkinBody()
                ))
        );

        automationField.ifPresent(field ->
                customFields.put(field.systemName(), 1)
        );

        long caseId = client.createCase(
                sectionId,
                candidate.scenarioTitle(),
                templateId,
                jiraRefs(candidate),
                customFields
        );

        validateCreatedCase(caseId);

        return SyncResult.replace(
                "@C" + caseId,
                "@sync"
        );
    }

    private String jiraRefs(SyncCandidate candidate) {
        return candidate.tags().stream()
                .filter(tag -> tag.matches("@[A-Z]{2,}-\\d+"))
                .map(tag -> tag.substring(1))
                .distinct()
                .reduce((first, second) -> first + ", " + second)
                .orElse(null);
    }

    private void validateCreatedCase(long caseId) {
        Map<String, Object> created = client.getCase(caseId);

        Object gherkinValue =
                created.get(gherkinField.systemName());

        if (gherkinValue == null ||
                String.valueOf(gherkinValue).isBlank()) {

            log.warn(
                    "C{}: Gherkin field '{}' came back empty",
                    caseId,
                    gherkinField.systemName()
            );
        }

        automationField.ifPresent(field -> {
            Object value = created.get(field.systemName());

            if (!"1".equals(String.valueOf(value))
                    && !Boolean.TRUE.equals(value)) {

                log.warn(
                        "C{}: automation flag did not stick (came back as '{}')",
                        caseId,
                        value
                );
            }
        });
    }
}
