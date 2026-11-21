package com.coreplatform.authorization.resolver;

import com.coreplatform.authorization.model.DataScope;
import com.coreplatform.authorization.model.UserAuthorizationData;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Objects;

@Slf4j
public class OrgScopeEvaluator implements ScopeEvaluator {

    @Override
    public String scopeName() {
        return DataScope.ORG;
    }

    @Override
    public boolean evaluate(UserAuthorizationData user, Map<String, Object> resourceAttributes) {
        Object resourceOrgId = resourceAttributes.get("orgId");
        if (resourceOrgId == null) {
            log.debug("Missing orgId attribute, denying (fail-closed)");
            return false;
        }
        return Objects.equals(resourceOrgId.toString(), user.getOrgId().toString());
    }
}
