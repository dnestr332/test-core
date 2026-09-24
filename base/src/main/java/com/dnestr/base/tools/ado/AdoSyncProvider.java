package com.dnestr.base.tools.ado;

import com.dnestr.base.tools.bddsync.SyncCandidate;
import com.dnestr.base.tools.bddsync.SyncProvider;
import com.dnestr.base.tools.bddsync.SyncResult;

/**
 * {@link SyncProvider} for Azure DevOps: claims scenarios matching the ADO tag convention (see
 * {@link AdoScenarioMetadataParser}) and syncs each one as an ADO test case — updating it in place
 * if it's already linked via an {@code @tc:} tag, otherwise creating a new one (in the sprint suite
 * named by its {@code @S:} tag, creating that suite first if needed) and tagging the scenario with
 * the new ID. Either way, links the test case to every PBI named by an {@code @us:} tag.
 */
public final class AdoSyncProvider implements SyncProvider {

    private final AdoTestCaseSyncClient client;
    private final AdoScenarioMetadataParser metadataParser;

    public AdoSyncProvider(AdoTestCaseSyncClient client) {
        this.client = client;
        this.metadataParser = new AdoScenarioMetadataParser();
    }

    /** Matches the ADO tag convention: exactly one {@code @S:} tag and at most one {@code @tc:} tag (the actual values are validated later, in {@link #sync}). */
    @Override
    public boolean shouldSync(SyncCandidate candidate) {
        return candidate.tagsWithPrefix("@S:").size() == 1
                && candidate.tagsWithPrefix("@tc:").size() <= 1;
    }

    /**
     * Updates the existing ADO test case if {@code candidate} already carries an {@code @tc:} tag,
     * otherwise creates a new one in its sprint's suite; either way links every {@code @us:} PBI
     * to it, then returns the tag change to write back into the feature file.
     */
    @Override
    public SyncResult sync(SyncCandidate candidate) {
        AdoScenarioMetadata metadata = metadataParser.parse(candidate);

        long testCaseId;
        SyncResult result;

        if (metadata.testCaseId().isPresent()) {
            testCaseId = metadata.testCaseId().get();

            client.updateTestCase(
                    testCaseId,
                    candidate.scenarioTitle(),
                    candidate.gherkinBody()
            );

            result = SyncResult.replace("@tc:" + testCaseId, "@tc:" + testCaseId);
        } else {
            int suiteId = client.resolveOrCreateSprintSuite(metadata.sprint());

            testCaseId = client.createTestCase(
                    candidate.scenarioTitle(),
                    candidate.gherkinBody()
            );

            client.addTestCaseToSuite(suiteId, testCaseId);

            result = SyncResult.add("@tc:" + testCaseId);
        }

        for (long pbiId : metadata.pbiIds()) {
            client.linkTestCaseToPbi(testCaseId, pbiId);
        }

        return result;
    }
}
