package com.coreplatform.authorization.resolver;

import com.coreplatform.authorization.model.DataScope;
import com.coreplatform.authorization.model.UserAuthorizationData;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Objects;

@Slf4j
public class OwnScopeEvaluator implements ScopeEvaluator {

    @Override
    public String scopeName() {
        return DataScope.OWN;
    }

    @Override
    public boolean evaluate(UserAuthorizationData user, Map<String, Object> resourceAttributes) {
        Object ownerId = resourceAttributes.get("ownerId");
        if (ownerId == null) {
            log.debug("Missing ownerId attribute, denying (fail-closed)");
            return false;
        }
        return Objects.equals(ownerId.toString(), user.getUserId().toString());
    }
}
