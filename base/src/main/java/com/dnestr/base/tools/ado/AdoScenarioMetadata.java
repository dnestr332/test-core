package com.dnestr.base.tools.ado;

import java.util.List;
import java.util.Optional;

/**
 * A scenario's ADO-relevant tags, parsed and validated by {@link AdoScenarioMetadataParser}.
 *
 * @param sprint     the sprint name from the scenario's required {@code @S:} tag
 * @param testCaseId the ADO test case work item ID from an existing {@code @tc:} tag, if the
 *                    scenario has already been synced before; empty if this will be a first sync
 * @param pbiIds     ADO work item IDs from the scenario's {@code @us:} tags, to link the test case to
 */
public record AdoScenarioMetadata(
        String sprint,
        Optional<Long> testCaseId,
        List<Long> pbiIds
) {}
