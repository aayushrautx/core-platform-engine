package com.coreplatform.authorization.resolver;

import com.coreplatform.authorization.model.UserAuthorizationData;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class ScopeEnforcer {

    private final Map<String, ScopeEvaluator> evaluatorsByScope;

    public ScopeEnforcer(List<ScopeEvaluator> evaluators) {
        this.evaluatorsByScope = evaluators.stream()
                .collect(Collectors.toMap(ScopeEvaluator::scopeName, Function.identity()));
        log.info("ScopeEnforcer initialized with scopes: {}", evaluatorsByScope.keySet());
    }

    public boolean enforce(UserAuthorizationData user, String scope, Map<String, Object> resourceAttributes) {
        if (scope == null) {
            log.debug("No scope to enforce, allowing");
            return true;
        }

        ScopeEvaluator evaluator = evaluatorsByScope.get(scope);
        if (evaluator == null) {
            log.warn("No ScopeEvaluator registered for scope={}, denying", scope);
            return false;
        }

        if (resourceAttributes == null || resourceAttributes.isEmpty()) {
            log.debug("Missing resource attributes for scope={}, denying (fail-closed)", scope);
            return false;
        }

        boolean result = evaluator.evaluate(user, resourceAttributes);
        log.debug("Scope enforcement: user={} scope={} result={}", user.getKeycloakSub(), scope, result);
        return result;
    }
}
