package com.coreplatform.authorization.resolver;

import com.coreplatform.authorization.model.DataScope;
import com.coreplatform.authorization.model.UserAuthorizationData;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Objects;

@Slf4j
public class DepartmentScopeEvaluator implements ScopeEvaluator {

    @Override
    public String scopeName() {
        return DataScope.DEPARTMENT;
    }

    @Override
    public boolean evaluate(UserAuthorizationData user, Map<String, Object> resourceAttributes) {
        Object resourceDepartment = resourceAttributes.get("department");
        if (resourceDepartment == null) {
            log.debug("Missing department attribute, denying (fail-closed)");
            return false;
        }
        return Objects.equals(resourceDepartment.toString(), user.getDepartment());
    }
}
