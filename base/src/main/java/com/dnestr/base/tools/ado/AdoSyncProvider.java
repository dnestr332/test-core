package com.dnestr.base.tools.ado;

import com.dnestr.base.tools.bddsync.SyncCandidate;
import com.dnestr.base.tools.bddsync.SyncProvider;
import com.dnestr.base.tools.bddsync.SyncResult;

public final class AdoSyncProvider implements SyncProvider {

    private final AdoTestCaseSyncClient client;
    private final AdoScenarioMetadataParser metadataParser;

    public AdoSyncProvider(AdoTestCaseSyncClient client) {
        this.client = client;
        this.metadataParser = new AdoScenarioMetadataParser();
    }

    @Override
    public boolean shouldSync(SyncCandidate candidate) {
        return candidate.tagsWithPrefix("@S:").size() == 1
                && candidate.tagsWithPrefix("@tc:").size() <= 1;
    }

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
