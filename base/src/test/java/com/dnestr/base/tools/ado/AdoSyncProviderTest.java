package com.dnestr.base.tools.ado;

import com.dnestr.base.tools.bddsync.SyncCandidate;
import com.dnestr.base.tools.bddsync.SyncResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdoSyncProviderTest {

    private static final String GHERKIN_BODY = "Given x\nWhen y\nThen z";

    private AdoTestCaseSyncClient client;
    private AdoSyncProvider provider;

    @BeforeEach
    void setUp() {
        client = Mockito.mock(AdoTestCaseSyncClient.class);
        provider = new AdoSyncProvider(client);
    }

    private SyncCandidate candidate(List<String> tags) {
        return new SyncCandidate(
                Path.of("features/foo.feature"),
                "My Feature",
                0,
                true,
                null,
                "My Scenario",
                GHERKIN_BODY,
                tags
        );
    }

    @Test
    void shouldSync_true_whenExactlyOneSprintTagAndNoTcTag() {
        assertThat(provider.shouldSync(candidate(List.of("@S:Sprint1")))).isTrue();
    }

    @Test
    void shouldSync_false_whenNoSprintTag() {
        assertThat(provider.shouldSync(candidate(List.of("@us:111")))).isFalse();
    }

    @Test
    void shouldSync_false_whenMultipleSprintTags() {
        assertThat(provider.shouldSync(
                candidate(List.of("@S:Sprint1", "@S:Sprint2"))
        )).isFalse();
    }

    @Test
    void shouldSync_true_whenAlreadyLinkedToAdoTestCase() {
        assertThat(provider.shouldSync(
                candidate(List.of("@S:Sprint1", "@tc:123"))
        )).isTrue();
    }

    @Test
    void shouldSync_true_whenAlreadyLinkedToAdoTestCase_lowercaseTag() {
        assertThat(provider.shouldSync(
                candidate(List.of("@S:Sprint1", "@TC:123"))
        )).isTrue();
    }

    @Test
    void shouldSync_false_whenMultipleTcTagsPresent() {
        assertThat(provider.shouldSync(
                candidate(List.of("@S:Sprint1", "@tc:123", "@tc:456"))
        )).isFalse();
    }

    @Test
    void sync_resolvesSuite_createsCase_addsToSuite_inOrder() {
        when(client.resolveOrCreateSprintSuite("Sprint1")).thenReturn(55);
        when(client.createTestCase("My Scenario", GHERKIN_BODY)).thenReturn(9L);

        provider.sync(candidate(List.of("@S:Sprint1")));

        InOrder order = inOrder(client);
        order.verify(client).resolveOrCreateSprintSuite("Sprint1");
        order.verify(client).createTestCase("My Scenario", GHERKIN_BODY);
        order.verify(client).addTestCaseToSuite(55, 9L);
    }

    @Test
    void sync_linksTestCaseToEveryDistinctPbi() {
        when(client.resolveOrCreateSprintSuite("Sprint1")).thenReturn(55);
        when(client.createTestCase("My Scenario", GHERKIN_BODY)).thenReturn(9L);

        provider.sync(candidate(List.of("@S:Sprint1", "@us:111", "@us:222", "@us:111")));

        verify(client).linkTestCaseToPbi(9L, 111L);
        verify(client).linkTestCaseToPbi(9L, 222L);
    }

    @Test
    void sync_doesNotLinkAnyPbi_whenNoUsTagsPresent() {
        when(client.resolveOrCreateSprintSuite("Sprint1")).thenReturn(55);
        when(client.createTestCase("My Scenario", GHERKIN_BODY)).thenReturn(9L);

        provider.sync(candidate(List.of("@S:Sprint1")));

        verify(client, never()).linkTestCaseToPbi(Mockito.anyLong(), Mockito.anyLong());
    }

    @Test
    void sync_returnsAddResult_withCreatedTestCaseIdTag() {
        when(client.resolveOrCreateSprintSuite("Sprint1")).thenReturn(55);
        when(client.createTestCase("My Scenario", GHERKIN_BODY)).thenReturn(9L);

        SyncResult result = provider.sync(candidate(List.of("@S:Sprint1")));

        assertThat(result).isEqualTo(SyncResult.add("@tc:9"));
    }

    @Test
    void sync_updatesExistingTestCase_whenTcTagAlreadyPresent() {
        provider.sync(candidate(List.of("@S:Sprint1", "@tc:9")));

        verify(client).updateTestCase(9L, "My Scenario", GHERKIN_BODY);
    }

    @Test
    void sync_doesNotResolveSuiteOrCreateOrAssociate_whenUpdatingExistingTestCase() {
        provider.sync(candidate(List.of("@S:Sprint1", "@tc:9")));

        verify(client, never()).resolveOrCreateSprintSuite(Mockito.any());
        verify(client, never()).createTestCase(Mockito.any(), Mockito.any());
        verify(client, never()).addTestCaseToSuite(Mockito.anyInt(), Mockito.anyLong());
    }

    @Test
    void sync_stillLinksPbis_whenUpdatingExistingTestCase() {
        provider.sync(candidate(List.of("@S:Sprint1", "@tc:9", "@us:111")));

        verify(client).linkTestCaseToPbi(9L, 111L);
    }

    @Test
    void sync_returnsReplaceResult_withSameTagAddedAndRemoved_whenUpdating() {
        SyncResult result = provider.sync(candidate(List.of("@S:Sprint1", "@tc:9")));

        assertThat(result).isEqualTo(SyncResult.replace("@tc:9", "@tc:9"));
    }
}
