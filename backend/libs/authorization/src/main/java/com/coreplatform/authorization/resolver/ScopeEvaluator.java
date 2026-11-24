package com.coreplatform.authorization.resolver;

import com.coreplatform.authorization.model.UserAuthorizationData;

import java.util.Map;

public interface ScopeEvaluator {

    String scopeName();

    boolean evaluate(UserAuthorizationData user, Map<String, Object> resourceAttributes);

    default Map<String, Object> toPredicateParams(UserAuthorizationData user) {
        return Map.of();
    }
}
