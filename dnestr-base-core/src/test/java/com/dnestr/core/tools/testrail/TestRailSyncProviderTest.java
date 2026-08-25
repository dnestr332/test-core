package com.dnestr.core.tools.testrail;

import com.dnestr.core.tools.bddsync.SyncCandidate;
import com.dnestr.core.tools.bddsync.SyncResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TestRailSyncProviderTest {

    private static final String GHERKIN_BODY = "Given x\nWhen y\nThen z";

    private TestRailCaseSyncClient client;
    private TestRailSyncConfig config;

    @BeforeEach
    void setUp() {
        client = Mockito.mock(TestRailCaseSyncClient.class);
        config = new TestRailSyncConfig(100, 5);

        when(client.resolveProjectId(100)).thenReturn(7);
        when(client.resolveGherkinTemplateId(7)).thenReturn(42);
        when(client.resolveRequiredField("testrail_bdd_scenario"))
                .thenReturn(new TestRailCaseSyncClient.CaseField("testrail_bdd_scenario", "BDD Scenario", 1));
    }

    private TestRailSyncProvider providerWithAutomationField() {
        when(client.resolveField("is_automated", "is automated"))
                .thenReturn(Optional.of(new TestRailCaseSyncClient.CaseField("automation_type", "Automation Type", 2)));
        return new TestRailSyncProvider(client, config);
    }

    private TestRailSyncProvider providerWithoutAutomationField() {
        when(client.resolveField("is_automated", "is automated")).thenReturn(Optional.empty());
        when(client.resolveField("automat")).thenReturn(Optional.empty());
        return new TestRailSyncProvider(client, config);
    }

    private SyncCandidate candidate(Path file, String featureTitle, String scenarioTitle, List<String> tags) {
        return new SyncCandidate(file, featureTitle, 0, true, null, scenarioTitle, GHERKIN_BODY, tags);
    }

    private SyncCandidate candidate(List<String> tags) {
        return candidate(Path.of("features/foo.feature"), "My Feature", "My Scenario", tags);
    }

    @Test
    void constructor_resolvesProjectTemplateAndGherkinField_fromConfiguredSuite() {
        providerWithAutomationField();

        verify(client).resolveProjectId(100);
        verify(client).resolveGherkinTemplateId(7);
        verify(client).resolveRequiredField("testrail_bdd_scenario");
    }

    @Test
    void constructor_fallsBackToSecondAutomationHint_whenFirstDoesNotMatch() {
        when(client.resolveField("is_automated", "is automated")).thenReturn(Optional.empty());
        when(client.resolveField("automat"))
                .thenReturn(Optional.of(new TestRailCaseSyncClient.CaseField("automated_flag", "Automated?", 2)));

        new TestRailSyncProvider(client, config);

        verify(client).resolveField("automat");
    }

    @Test
    void shouldSync_true_whenSyncTagPresentAndNoCaseIdYet() {
        TestRailSyncProvider provider = providerWithAutomationField();

        assertThat(provider.shouldSync(candidate(List.of("@sync", "@S:Sprint1")))).isTrue();
    }

    @Test
    void shouldSync_false_whenSyncTagMissing() {
        TestRailSyncProvider provider = providerWithAutomationField();

        assertThat(provider.shouldSync(candidate(List.of("@S:Sprint1")))).isFalse();
    }

    @Test
    void shouldSync_true_whenAlreadyLinkedToTestRailCase() {
        TestRailSyncProvider provider = providerWithAutomationField();

        assertThat(provider.shouldSync(candidate(List.of("@sync", "@C123")))).isTrue();
    }

    @Test
    void shouldSync_true_whenAlreadyLinkedToTestRailCase_lowercaseTag() {
        TestRailSyncProvider provider = providerWithAutomationField();

        assertThat(provider.shouldSync(candidate(List.of("@sync", "@c456")))).isTrue();
    }

    @Test
    void shouldSync_false_whenMultipleCaseTagsPresent() {
        TestRailSyncProvider provider = providerWithAutomationField();

        assertThat(provider.shouldSync(candidate(List.of("@sync", "@C123", "@C456")))).isFalse();
    }

    @Test
    void sync_reusesSubsectionId_forCandidatesInTheSameFile() {
        TestRailSyncProvider provider = providerWithAutomationField();
        Path file = Path.of("features/foo.feature");

        when(client.resolveOrCreateSubsection(7, 100, 5, "My Feature")).thenReturn(55);
        when(client.createCase(eq(55), any(), eq(42), any(), any())).thenReturn(1L, 2L);
        when(client.getCase(anyLong())).thenReturn(Map.of());

        provider.sync(candidate(file, "My Feature", "Scenario A", List.of("@sync")));
        provider.sync(candidate(file, "My Feature", "Scenario B", List.of("@sync")));

        verify(client, times(1)).resolveOrCreateSubsection(7, 100, 5, "My Feature");
    }

    @Test
    @SuppressWarnings("unchecked")
    void sync_setsAutomationFlag_whenAutomationFieldIsPresent() {
        TestRailSyncProvider provider = providerWithAutomationField();

        when(client.resolveOrCreateSubsection(7, 100, 5, "My Feature")).thenReturn(55);
        when(client.createCase(eq(55), eq("My Scenario"), eq(42), isNull(), any())).thenReturn(9L);
        when(client.getCase(9L)).thenReturn(Map.of("automation_type", "1", "testrail_bdd_scenario", "body"));

        provider.sync(candidate(List.of("@sync")));

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(client).createCase(eq(55), eq("My Scenario"), eq(42), isNull(), captor.capture());

        Map<String, Object> fields = captor.getValue();
        assertThat(fields).containsEntry("automation_type", 1);
        assertThat(fields).containsEntry("testrail_bdd_scenario",
                List.of(Map.of("content", GHERKIN_BODY)));
    }

    @Test
    @SuppressWarnings("unchecked")
    void sync_escapesGherkinOutlinePlaceholders_soTheyAreNotParsedAsHtmlTags() {
        TestRailSyncProvider provider = providerWithAutomationField();

        String outlineBody = "Scenario Outline: <usertype> cancels future ride\n"
                + "Given <usertype> is logged in to the Dashboard";

        SyncCandidate candidate = new SyncCandidate(
                Path.of("features/foo.feature"), "My Feature", 0, true, null,
                "My Scenario", outlineBody, List.of("@sync")
        );

        when(client.resolveOrCreateSubsection(7, 100, 5, "My Feature")).thenReturn(55);
        when(client.createCase(eq(55), any(), eq(42), any(), any())).thenReturn(9L);
        when(client.getCase(9L)).thenReturn(Map.of());

        provider.sync(candidate);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(client).createCase(eq(55), eq("My Scenario"), eq(42), isNull(), captor.capture());

        List<Map<String, Object>> gherkinSteps =
                (List<Map<String, Object>>) captor.getValue().get("testrail_bdd_scenario");
        String content = (String) gherkinSteps.get(0).get("content");

        assertThat(content)
                .doesNotContain("<usertype>")
                .contains("&lt;usertype&gt;");
    }

    @Test
    @SuppressWarnings("unchecked")
    void sync_preservesAndAndAsteriskSteps_unchangedAsideFromPlaceholderEscaping() {
        TestRailSyncProvider provider = providerWithAutomationField();

        String outlineBody = "Scenario Outline: <UserType> cancels future ride\n"
                + "    * system creates Regular ride for Kinship Test passenger with Facility method\n"
                + "    Given <UserType> is logged in to the Dashboard\n"
                + "    When user navigates to the \"Facility Passenger\" path\n"
                + "    And user saves the count of \"Canceled Rides\" field\n"
                + "    * system cancels an active ride\n"
                + "    * system waits for 10 seconds\n"
                + "    * page is refreshed 1 times\n"
                + "    And the count of \"Canceled Rides\" field should change by 1";

        SyncCandidate candidate = new SyncCandidate(
                Path.of("features/foo.feature"), "My Feature", 0, true, null,
                "My Scenario", outlineBody, List.of("@sync")
        );

        when(client.resolveOrCreateSubsection(7, 100, 5, "My Feature")).thenReturn(55);
        when(client.createCase(eq(55), any(), eq(42), any(), any())).thenReturn(9L);
        when(client.getCase(9L)).thenReturn(Map.of());

        provider.sync(candidate);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(client).createCase(eq(55), eq("My Scenario"), eq(42), isNull(), captor.capture());

        List<Map<String, Object>> gherkinSteps =
                (List<Map<String, Object>>) captor.getValue().get("testrail_bdd_scenario");
        String content = (String) gherkinSteps.get(0).get("content");

        assertThat(content)
                .isEqualTo(outlineBody.replace("<UserType>", "&lt;UserType&gt;"))
                .contains("* system creates Regular ride")
                .contains("* system cancels an active ride")
                .contains("* system waits for 10 seconds")
                .contains("* page is refreshed 1 times")
                .contains("And user saves the count of \"Canceled Rides\" field")
                .contains("And the count of \"Canceled Rides\" field should change by 1");
    }

    @Test
    @SuppressWarnings("unchecked")
    void sync_omitsAutomationFlag_whenAutomationFieldIsAbsent() {
        TestRailSyncProvider provider = providerWithoutAutomationField();

        when(client.resolveOrCreateSubsection(7, 100, 5, "My Feature")).thenReturn(55);
        when(client.createCase(eq(55), eq("My Scenario"), eq(42), isNull(), any())).thenReturn(9L);
        when(client.getCase(9L)).thenReturn(Map.of());

        provider.sync(candidate(List.of("@sync")));

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(client).createCase(eq(55), eq("My Scenario"), eq(42), isNull(), captor.capture());

        assertThat(captor.getValue()).containsOnlyKeys("testrail_bdd_scenario");
    }

    @Test
    void sync_returnsReplaceResult_withCreatedCaseIdAndSyncTagRemoved() {
        TestRailSyncProvider provider = providerWithAutomationField();

        when(client.resolveOrCreateSubsection(7, 100, 5, "My Feature")).thenReturn(55);
        when(client.createCase(eq(55), eq("My Scenario"), eq(42), isNull(), any())).thenReturn(9L);
        when(client.getCase(9L)).thenReturn(Map.of());

        SyncResult result = provider.sync(candidate(List.of("@sync")));

        assertThat(result).isEqualTo(SyncResult.replace("@C9", "@sync"));
    }

    @Test
    void sync_updatesExistingCase_whenSyncTagAddedBackToAnAlreadyLinkedScenario() {
        TestRailSyncProvider provider = providerWithAutomationField();

        when(client.getCase(9L)).thenReturn(Map.of());

        provider.sync(candidate(List.of("@sync", "@C9")));

        verify(client).updateCase(9L, "My Scenario", 42, null, Map.of(
                "testrail_bdd_scenario", List.of(Map.of("content", GHERKIN_BODY)),
                "automation_type", 1
        ));
    }

    @Test
    void sync_doesNotCreateOrResolveSubsection_whenUpdatingExistingCase() {
        TestRailSyncProvider provider = providerWithAutomationField();

        when(client.getCase(9L)).thenReturn(Map.of());

        provider.sync(candidate(List.of("@sync", "@C9")));

        verify(client, Mockito.never()).resolveOrCreateSubsection(anyInt(), anyInt(), anyInt(), any());
        verify(client, Mockito.never()).createCase(anyInt(), any(), anyInt(), any(), any());
    }

    @Test
    void sync_returnsReplaceResult_removingSyncAndOldCaseTag_whenUpdating() {
        TestRailSyncProvider provider = providerWithAutomationField();

        when(client.getCase(9L)).thenReturn(Map.of());

        SyncResult result = provider.sync(candidate(List.of("@sync", "@C9")));

        assertThat(result.tagToAdd()).isEqualTo("@C9");
        assertThat(result.tagsToRemove()).containsExactlyInAnyOrder("@sync", "@C9");
    }

    @Test
    void sync_updatesExistingCase_whenCaseTagIsLowercase() {
        TestRailSyncProvider provider = providerWithAutomationField();

        when(client.getCase(9L)).thenReturn(Map.of());

        provider.sync(candidate(List.of("@sync", "@c9")));

        verify(client).updateCase(eq(9L), any(), anyInt(), any(), any());
    }

    @Test
    void sync_buildsJiraRefs_fromMatchingTags_distinctAndJoined() {
        TestRailSyncProvider provider = providerWithAutomationField();

        when(client.resolveOrCreateSubsection(7, 100, 5, "My Feature")).thenReturn(55);
        when(client.createCase(eq(55), any(), eq(42), any(), any())).thenReturn(9L);
        when(client.getCase(9L)).thenReturn(Map.of());

        provider.sync(candidate(List.of("@sync", "@JIRA-123", "@ABCD-99", "@JIRA-123")));

        verify(client).createCase(eq(55), eq("My Scenario"), eq(42), eq("JIRA-123, ABCD-99"), any());
    }

    @Test
    void sync_passesNullRefs_whenNoJiraTagsPresent() {
        TestRailSyncProvider provider = providerWithAutomationField();

        when(client.resolveOrCreateSubsection(7, 100, 5, "My Feature")).thenReturn(55);
        when(client.createCase(eq(55), any(), eq(42), any(), any())).thenReturn(9L);
        when(client.getCase(9L)).thenReturn(Map.of());

        provider.sync(candidate(List.of("@sync")));

        verify(client).createCase(eq(55), eq("My Scenario"), eq(42), isNull(), any());
    }

    @Test
    void sync_doesNotThrow_whenCreatedCaseComesBackMissingGherkinContent() {
        TestRailSyncProvider provider = providerWithAutomationField();

        when(client.resolveOrCreateSubsection(7, 100, 5, "My Feature")).thenReturn(55);
        when(client.createCase(eq(55), any(), eq(42), any(), any())).thenReturn(9L);
        when(client.getCase(9L)).thenReturn(Map.of("testrail_bdd_scenario", ""));

        SyncResult result = provider.sync(candidate(List.of("@sync")));

        assertThat(result).isNotNull();
        verify(client).getCase(9L);
    }
}
