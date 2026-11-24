package com.coreplatform.authorization.policy;

import com.coreplatform.authorization.model.PolicyEffect;

import java.util.List;

public class DenyOverridesStrategy implements CombiningStrategy {

    @Override
    public PolicyResult combine(List<PolicyResult> results) {
        PolicyResult lastPermit = null;

        for (PolicyResult result : results) {
            if (!result.isApplicable()) continue;

            if (result.getEffect() == PolicyEffect.DENY) {
                return result;
            }
            if (result.getEffect() == PolicyEffect.PERMIT) {
                lastPermit = result;
            }
        }

        return lastPermit != null ? lastPermit : PolicyResult.notApplicable();
    }
}
