package com.coreplatform.authorization.resolver;

import com.coreplatform.authorization.model.UserAuthorizationData;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class DataScopeResolver {

    private final Map<String, ScopeEvaluator> evaluatorsByScope;

    public DataScopeResolver(List<ScopeEvaluator> evaluators) {
        this.evaluatorsByScope = evaluators.stream()
                .collect(Collectors.toMap(ScopeEvaluator::scopeName, Function.identity()));
        log.info("DataScopeResolver initialized with scopes: {}", evaluatorsByScope.keySet());
    }

    public Map<String, Object> resolvePredicateParams(UserAuthorizationData user, String scope) {
        if (scope == null || "ALL".equals(scope)) {
            return Map.of();
        }

        Map<String, Object> params = new HashMap<>();

        switch (scope) {
            case "OWN":
                params.put("ownerKeycloakSub", user.getKeycloakSub());
                params.put("ownerId", user.getUserId());
                break;
            case "DEPARTMENT":
                params.put("department", user.getDepartment());
                params.put("tenantId", user.getTenantId());
                break;
            case "ORG":
                params.put("orgId", user.getOrgId());
                params.put("tenantId", user.getTenantId());
                break;
            default:
                log.warn("Unknown scope for predicate: {}, defaulting to empty", scope);
        }

        return params;
    }

    public String buildWhereClause(String scope, String tableAlias) {
        if (scope == null || "ALL".equals(scope)) {
            return "1=1";
        }

        String prefix = tableAlias != null ? tableAlias + "." : "";

        switch (scope) {
            case "OWN":
                return prefix + "owner_keycloak_sub = :ownerKeycloakSub";
            case "DEPARTMENT":
                return prefix + "tenant_id = :tenantId AND " + prefix + "department = :department";
            case "ORG":
                return prefix + "tenant_id = :tenantId AND " + prefix + "org_id = :orgId";
            default:
                log.warn("Unknown scope for where clause: {}, denying all", scope);
                return "1=0";
        }
    }

    public ScopeFilter toFilter(UserAuthorizationData user, String scope) {
        return new ScopeFilter(
                scope,
                buildWhereClause(scope, null),
                resolvePredicateParams(user, scope)
        );
    }

    public record ScopeFilter(String scope, String whereClause, Map<String, Object> params) {}
}
