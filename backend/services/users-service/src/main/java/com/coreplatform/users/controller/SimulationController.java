package com.coreplatform.users.controller;

import com.coreplatform.authorization.annotation.Authorize;
import com.coreplatform.authorization.core.AuthorizationContext;
import com.coreplatform.authorization.core.AuthorizationDecision;
import com.coreplatform.authorization.engine.AuthorizationEngine;
import com.coreplatform.authorization.model.Policy;
import com.coreplatform.authorization.model.PrincipalType;
import com.coreplatform.authorization.policy.CompiledPolicy;
import com.coreplatform.authorization.policy.ConditionEvaluator;
import com.coreplatform.authorization.policy.PolicyCompiler;
import com.coreplatform.authorization.policy.PolicyCompilationException;
import com.coreplatform.authorization.provider.AuthorizationDataProvider;
import com.coreplatform.users.dto.SimulationRequest;
import com.coreplatform.users.dto.SimulationResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api/authorization")
public class SimulationController {

    private final AuthorizationEngine authorizationEngine;
    private final AuthorizationDataProvider dataProvider;
    private final PolicyCompiler policyCompiler;
    private final ConditionEvaluator conditionEvaluator;

    public SimulationController(AuthorizationEngine authorizationEngine,
                                 AuthorizationDataProvider dataProvider,
                                 PolicyCompiler policyCompiler,
                                 ConditionEvaluator conditionEvaluator) {
        this.authorizationEngine = authorizationEngine;
        this.dataProvider = dataProvider;
        this.policyCompiler = policyCompiler;
        this.conditionEvaluator = conditionEvaluator;
    }

    @PostMapping("/simulate")
    @Authorize(resource = "authorization", action = "simulate")
    public ResponseEntity<SimulationResponse> simulate(@RequestBody SimulationRequest request) {
        AuthorizationContext context = AuthorizationContext.builder()
                .principal(AuthorizationContext.Principal.builder()
                        .type(PrincipalType.USER)
                        .sub(request.userKeycloakSub())
                        .build())
                .tenantId(request.tenantId())
                .resource(request.resource())
                .action(request.action())
                .resourceAttributes(request.resourceAttributes())
                .subjectAttributes(request.subjectAttributes())
                .environmentAttributes(request.environmentAttributes())
                .build();

        AuthorizationDecision decision = authorizationEngine.authorize(context);

        List<SimulationResponse.PolicyEvaluationDetail> policyDetails = buildPolicyDetails(
                request.tenantId(), request.resource(), request.action(),
                request.resourceAttributes(), request.subjectAttributes(),
                request.environmentAttributes());

        String tenantCheck = decision.getReason() != null && decision.getReason().contains("Tenant")
                ? "FAIL" : "PASS";

        SimulationResponse response = new SimulationResponse(
                decision.isAllowed(),
                decision.isRbacResult(),
                decision.getScopeResult() != null ? decision.getScopeResult() : "N/A",
                tenantCheck,
                policyDetails,
                decision.getMatchedPolicy(),
                decision.getReason(),
                decision.getObligations()
        );

        return ResponseEntity.ok(response);
    }

    private List<SimulationResponse.PolicyEvaluationDetail> buildPolicyDetails(
            UUID tenantId, String resource, String action,
            Map<String, Object> resourceAttrs, Map<String, Object> subjectAttrs,
            Map<String, Object> envAttrs) {

        List<Policy> policies = dataProvider.getPolicies(tenantId, resource, action);
        if (policies == null || policies.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Object> evalContext = new HashMap<>();
        if (resourceAttrs != null) evalContext.putAll(resourceAttrs);
        if (subjectAttrs != null) evalContext.putAll(subjectAttrs);
        if (envAttrs != null) evalContext.putAll(envAttrs);
        evalContext.put("environment.time", System.currentTimeMillis());

        List<SimulationResponse.PolicyEvaluationDetail> details = new ArrayList<>();

        for (Policy policy : policies) {
            if (!policy.isActive()) continue;

            boolean matched = false;
            List<String> missingAttributes = new ArrayList<>();

            try {
                CompiledPolicy compiled = policyCompiler.compile(policy);
                matched = conditionEvaluator.evaluate(compiled.getConditionTree(), evalContext);
                missingAttributes = findMissingAttributes(policy.getConditionsJson(), evalContext);
            } catch (PolicyCompilationException e) {
                missingAttributes.add("COMPILATION_ERROR: " + e.getMessage());
            }

            details.add(new SimulationResponse.PolicyEvaluationDetail(
                    policy.getName(),
                    policy.getEffect().name(),
                    policy.isOverrideRbac(),
                    matched,
                    missingAttributes
            ));
        }

        return details;
    }

    private List<String> findMissingAttributes(String conditionsJson, Map<String, Object> context) {
        if (conditionsJson == null || conditionsJson.isBlank()) {
            return Collections.emptyList();
        }

        List<String> missing = new ArrayList<>();
        Set<String> knownPrefixes = Set.of("subject.", "resource.", "environment.");

        for (String prefix : knownPrefixes) {
            int idx = 0;
            while ((idx = conditionsJson.indexOf("\"" + prefix, idx)) != -1) {
                int start = idx + 1;
                int end = conditionsJson.indexOf("\"", start);
                if (end > start) {
                    String attr = conditionsJson.substring(start, end);
                    if (!context.containsKey(attr)) {
                        missing.add(attr);
                    }
                }
                idx = end > idx ? end + 1 : idx + 1;
            }
        }

        return missing.stream().distinct().toList();
    }
}
