package com.coreplatform.users.dto;

import java.util.List;
import java.util.Map;

public record SimulationResponse(
        boolean allowed,
        boolean rbacResult,
        String scopeResult,
        String tenantCheckResult,
        List<PolicyEvaluationDetail> policiesEvaluated,
        String matchedPolicy,
        String finalReason,
        List<Map<String, Object>> obligations
) {
    public record PolicyEvaluationDetail(
            String name,
            String effect,
            boolean overrideRbac,
            boolean matched,
            List<String> missingAttributes
    ) {}
}
