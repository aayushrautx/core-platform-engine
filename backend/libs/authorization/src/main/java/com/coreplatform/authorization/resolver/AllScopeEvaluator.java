package com.coreplatform.authorization.resolver;

import com.coreplatform.authorization.model.DataScope;
import com.coreplatform.authorization.model.UserAuthorizationData;

import java.util.Map;

public class AllScopeEvaluator implements ScopeEvaluator {

    @Override
    public String scopeName() {
        return DataScope.ALL;
    }

    @Override
    public boolean evaluate(UserAuthorizationData user, Map<String, Object> resourceAttributes) {
        return true;
    }
}
