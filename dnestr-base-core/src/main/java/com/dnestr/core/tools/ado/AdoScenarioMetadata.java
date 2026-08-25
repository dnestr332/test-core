package com.dnestr.core.tools.ado;

import java.util.List;
import java.util.Optional;

public record AdoScenarioMetadata(
        String sprint,
        Optional<Long> testCaseId,
        List<Long> pbiIds
) {}
