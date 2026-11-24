package com.coreplatform.authorization.policy;

import com.coreplatform.authorization.model.PolicyEffect;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class PolicyResult {

    private final PolicyEffect effect;
    private final String matchedPolicyName;
    private final List<Map<String, Object>> obligations;
    private final boolean applicable;

    public static PolicyResult notApplicable() {
        return PolicyResult.builder()
                .applicable(false)
                .build();
    }

    public static PolicyResult permit(String policyName, List<Map<String, Object>> obligations) {
        return PolicyResult.builder()
                .effect(PolicyEffect.PERMIT)
                .matchedPolicyName(policyName)
                .obligations(obligations)
                .applicable(true)
                .build();
    }

    public static PolicyResult deny(String policyName) {
        return PolicyResult.builder()
                .effect(PolicyEffect.DENY)
                .matchedPolicyName(policyName)
                .applicable(true)
                .build();
    }
}
