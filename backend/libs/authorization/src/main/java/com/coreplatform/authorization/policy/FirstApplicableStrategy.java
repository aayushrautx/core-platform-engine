package com.coreplatform.authorization.policy;

import java.util.List;

public class FirstApplicableStrategy implements CombiningStrategy {

    @Override
    public PolicyResult combine(List<PolicyResult> results) {
        for (PolicyResult result : results) {
            if (result.isApplicable()) {
                return result;
            }
        }
        return PolicyResult.notApplicable();
    }
}
