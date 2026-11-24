package com.coreplatform.authorization.policy;

import com.coreplatform.authorization.model.CombiningAlgorithm;
import com.coreplatform.authorization.model.Policy;
import com.coreplatform.authorization.model.PolicyEffect;
import com.coreplatform.authorization.provider.AuthorizationDataProvider;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class PolicyEvaluator {

    private static final int MAX_POLICIES_PER_REQUEST = 25;
    private static final TypeReference<List<Map<String, Object>>> OBLIGATIONS_TYPE = new TypeReference<>() {};

    private final AuthorizationDataProvider dataProvider;
    private final PolicyCompiler compiler;
    private final ConditionEvaluator conditionEvaluator;
    private final ObjectMapper objectMapper;
    private final Map<CombiningAlgorithm, CombiningStrategy> strategies;
    private final Timer evaluationTimer;

    public PolicyEvaluator(AuthorizationDataProvider dataProvider,
                           PolicyCompiler compiler,
                           ConditionEvaluator conditionEvaluator,
                           ObjectMapper objectMapper,
                           MeterRegistry meterRegistry) {
        this.dataProvider = dataProvider;
        this.compiler = compiler;
        this.conditionEvaluator = conditionEvaluator;
        this.objectMapper = objectMapper;
        this.strategies = Map.of(
                CombiningAlgorithm.DENY_OVERRIDES, new DenyOverridesStrategy(),
                CombiningAlgorithm.PERMIT_OVERRIDES, new PermitOverridesStrategy(),
                CombiningAlgorithm.FIRST_APPLICABLE, new FirstApplicableStrategy()
        );
        this.evaluationTimer = meterRegistry.timer("authz.policy.evaluation.duration");
    }

    public PolicyResult evaluate(UUID tenantId, String resource, String action,
                                  Map<String, Object> context, boolean rbacGranted) {
        return evaluationTimer.record(() ->
                doEvaluate(tenantId, resource, action, context, rbacGranted, null));
    }

    public PolicyResult evaluate(UUID tenantId, String resource, String action,
                                  Map<String, Object> context, boolean rbacGranted,
                                  CombiningAlgorithm algorithm) {
        return evaluationTimer.record(() ->
                doEvaluate(tenantId, resource, action, context, rbacGranted, algorithm));
    }

    private PolicyResult doEvaluate(UUID tenantId, String resource, String action,
                                     Map<String, Object> context, boolean rbacGranted,
                                     CombiningAlgorithm algorithm) {
        List<Policy> policies = dataProvider.getPolicies(tenantId, resource, action);
        if (policies == null || policies.isEmpty()) {
            return PolicyResult.notApplicable();
        }

        List<Policy> activePolicies = policies.stream()
                .filter(Policy::isActive)
                .sorted(Comparator.comparingInt(Policy::getPriority))
                .limit(MAX_POLICIES_PER_REQUEST)
                .collect(Collectors.toList());

        if (activePolicies.isEmpty()) {
            return PolicyResult.notApplicable();
        }

        if (algorithm == null) {
            algorithm = activePolicies.stream()
                    .map(Policy::getCombiningAlgorithm)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(CombiningAlgorithm.DENY_OVERRIDES);
        }

        List<PolicyResult> results = new ArrayList<>();

        for (Policy policy : activePolicies) {
            if (!rbacGranted && !policy.isOverrideRbac()) {
                continue;
            }

            try {
                CompiledPolicy compiled = compiler.compile(policy);
                Map<String, Object> evalContext = buildEvaluationContext(context);
                boolean conditionsMet = conditionEvaluator.evaluate(compiled.getConditionTree(), evalContext);

                if (conditionsMet) {
                    List<Map<String, Object>> obligations = parseObligations(policy.getObligationsJson());
                    if (compiled.getEffect() == PolicyEffect.PERMIT) {
                        results.add(PolicyResult.permit(compiled.getName(), obligations));
                    } else {
                        results.add(PolicyResult.deny(compiled.getName()));
                    }
                }
            } catch (PolicyCompilationException e) {
                log.warn("Policy compilation failed for policy={}, skipping: {}", policy.getId(), e.getMessage());
            }
        }

        if (results.isEmpty()) {
            return PolicyResult.notApplicable();
        }

        CombiningStrategy strategy = strategies.getOrDefault(algorithm, strategies.get(CombiningAlgorithm.DENY_OVERRIDES));
        return strategy.combine(results);
    }

    private Map<String, Object> buildEvaluationContext(Map<String, Object> context) {
        if (context == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> evalContext = new HashMap<>(context);
        evalContext.put("environment.time", System.currentTimeMillis());
        return evalContext;
    }

    private List<Map<String, Object>> parseObligations(String obligationsJson) {
        if (obligationsJson == null || obligationsJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(obligationsJson, OBLIGATIONS_TYPE);
        } catch (Exception e) {
            log.warn("Failed to parse obligations JSON: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
