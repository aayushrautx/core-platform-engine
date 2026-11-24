package com.coreplatform.authorization.policy;

import com.coreplatform.authorization.model.PolicyEffect;

import java.util.List;

public class PermitOverridesStrategy implements CombiningStrategy {

    @Override
    public PolicyResult combine(List<PolicyResult> results) {
        PolicyResult lastDeny = null;

        for (PolicyResult result : results) {
            if (!result.isApplicable()) continue;

            if (result.getEffect() == PolicyEffect.PERMIT) {
                return result;
            }
            if (result.getEffect() == PolicyEffect.DENY) {
                lastDeny = result;
            }
        }

        return lastDeny != null ? lastDeny : PolicyResult.notApplicable();
    }
}
