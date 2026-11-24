package com.coreplatform.authorization.engine;

import com.coreplatform.authorization.audit.AuditEntry;
import com.coreplatform.authorization.audit.AuditLogger;
import com.coreplatform.authorization.core.AuthorizationContext;
import com.coreplatform.authorization.core.AuthorizationDecision;
import com.coreplatform.authorization.guard.TenantGuard;
import com.coreplatform.authorization.model.PolicyEffect;
import com.coreplatform.authorization.model.PrincipalType;
import com.coreplatform.authorization.model.ServiceAccountData;
import com.coreplatform.authorization.model.UserAuthorizationData;
import com.coreplatform.authorization.policy.PolicyEvaluator;
import com.coreplatform.authorization.policy.PolicyResult;
import com.coreplatform.authorization.provider.AuthorizationDataProvider;
import com.coreplatform.authorization.rbac.RbacEvaluator;
import com.coreplatform.authorization.rbac.RbacResult;
import com.coreplatform.authorization.resolver.ScopeEnforcer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
public class AuthorizationEngine {

    private final AuthorizationDataProvider dataProvider;
    private final TenantGuard tenantGuard;
    private final RbacEvaluator rbacEvaluator;
    private final ScopeEnforcer scopeEnforcer;
    private final PolicyEvaluator policyEvaluator;
    private final AuditLogger auditLogger;
    private final Timer decisionTimer;

    public AuthorizationEngine(AuthorizationDataProvider dataProvider,
                               TenantGuard tenantGuard,
                               RbacEvaluator rbacEvaluator,
                               ScopeEnforcer scopeEnforcer,
                               PolicyEvaluator policyEvaluator,
                               AuditLogger auditLogger,
                               MeterRegistry meterRegistry) {
        this.dataProvider = dataProvider;
        this.tenantGuard = tenantGuard;
        this.rbacEvaluator = rbacEvaluator;
        this.scopeEnforcer = scopeEnforcer;
        this.policyEvaluator = policyEvaluator;
        this.auditLogger = auditLogger;
        this.decisionTimer = meterRegistry.timer("authz.decision.duration");
    }

    public AuthorizationDecision authorize(AuthorizationContext context) {
        return decisionTimer.record(() -> {
            PrincipalType principalType = context.getPrincipal().getType();

            if (principalType == PrincipalType.SERVICE_ACCOUNT) {
                return authorizeServiceAccount(context);
            }

            return authorizeUser(context);
        });
    }

    private AuthorizationDecision authorizeUser(AuthorizationContext context) {
        String sub = context.getPrincipal().getSub();

        Optional<UserAuthorizationData> userData = dataProvider.getUserData(sub);
        if (userData.isEmpty()) {
            return auditAndReturn(context, PrincipalType.USER, false, false, null, "Unknown user: " + sub);
        }

        UserAuthorizationData user = userData.get();

        tenantGuard.check(user, context.getTenantId());

        if (!user.isActive()) {
            return auditAndReturn(context, PrincipalType.USER, false, false, null, "Inactive user: " + sub);
        }

        RbacResult rbac = rbacEvaluator.hasPermission(user, context.getResource(), context.getAction());
        boolean rbacGranted = rbac.granted();

        if (!rbacGranted) {
            PolicyResult policyOverride = evaluatePolicies(context, user, false);
            if (policyOverride.isApplicable() && policyOverride.getEffect() == PolicyEffect.PERMIT) {
                return auditAndReturn(context, PrincipalType.USER, true, false, null,
                        "Policy override granted: " + policyOverride.getMatchedPolicyName(),
                        policyOverride.getMatchedPolicyName(), policyOverride.getObligations());
            }
            return auditAndReturn(context, PrincipalType.USER, false, false, null,
                    "No permission: " + context.getResource() + ":" + context.getAction());
        }

        if (context.getResourceId() != null && context.getResourceAttributes() != null) {
            boolean scopeAllowed = scopeEnforcer.enforce(user, rbac.scope(), context.getResourceAttributes());
            if (!scopeAllowed) {
                return auditAndReturn(context, PrincipalType.USER, false, true, rbac.scope(),
                        "Scope violation: " + rbac.scope());
            }
        }

        PolicyResult policyResult = evaluatePolicies(context, user, true);
        if (policyResult.isApplicable() && policyResult.getEffect() == PolicyEffect.DENY) {
            return auditAndReturn(context, PrincipalType.USER, false, true, rbac.scope(),
                    "Policy denied: " + policyResult.getMatchedPolicyName(),
                    policyResult.getMatchedPolicyName(), null);
        }

        return auditAndReturn(context, PrincipalType.USER, true, true, rbac.scope(),
                "RBAC granted with scope: " + rbac.scope(),
                policyResult.isApplicable() ? policyResult.getMatchedPolicyName() : null,
                policyResult.isApplicable() ? policyResult.getObligations() : null);
    }

    private AuthorizationDecision authorizeServiceAccount(AuthorizationContext context) {
        String sub = context.getPrincipal().getSub();

        Optional<ServiceAccountData> saData = dataProvider.getServiceAccountData(sub);
        if (saData.isEmpty()) {
            return auditAndReturn(context, PrincipalType.SERVICE_ACCOUNT, false, false, null,
                    "Unknown service account: " + sub);
        }

        ServiceAccountData sa = saData.get();

        String permissionKey = context.getResource() + ":" + context.getAction();
        if (sa.getEffectivePermissions() == null || !sa.getEffectivePermissions().contains(permissionKey)) {
            return auditAndReturn(context, PrincipalType.SERVICE_ACCOUNT, false, false, null,
                    "No permission: " + permissionKey);
        }

        return auditAndReturn(context, PrincipalType.SERVICE_ACCOUNT, true, true, null,
                "Service account RBAC granted: " + permissionKey);
    }

    private PolicyResult evaluatePolicies(AuthorizationContext context, UserAuthorizationData user, boolean rbacGranted) {
        if (policyEvaluator == null) {
            return PolicyResult.notApplicable();
        }

        try {
            Map<String, Object> policyContext = new HashMap<>();
            if (context.getResourceAttributes() != null) {
                policyContext.putAll(context.getResourceAttributes());
            }
            if (context.getSubjectAttributes() != null) {
                policyContext.putAll(context.getSubjectAttributes());
            } else {
                policyContext.put("subject.userId", user.getUserId());
                policyContext.put("subject.department", user.getDepartment());
                policyContext.put("subject.designation", user.getDesignation());
                policyContext.put("subject.tenantId", user.getTenantId());
            }
            if (context.getEnvironmentAttributes() != null) {
                policyContext.putAll(context.getEnvironmentAttributes());
            }
            return policyEvaluator.evaluate(context.getTenantId(), context.getResource(),
                    context.getAction(), policyContext, rbacGranted);
        } catch (Exception e) {
            log.error("Policy evaluation failed (fail-open for RBAC-granted, fail-closed otherwise): {}", e.getMessage());
            return PolicyResult.notApplicable();
        }
    }

    private AuthorizationDecision auditAndReturn(AuthorizationContext context,
                                                  PrincipalType principalType,
                                                  boolean allowed,
                                                  boolean rbacResult,
                                                  String scopeResult,
                                                  String reason) {
        return auditAndReturn(context, principalType, allowed, rbacResult, scopeResult, reason, null, null);
    }

    private AuthorizationDecision auditAndReturn(AuthorizationContext context,
                                                  PrincipalType principalType,
                                                  boolean allowed,
                                                  boolean rbacResult,
                                                  String scopeResult,
                                                  String reason,
                                                  String matchedPolicy,
                                                  java.util.List<Map<String, Object>> obligations) {
        try {
            auditLogger.log(AuditEntry.builder()
                    .userId(parseUserId(context.getPrincipal().getSub()))
                    .principalType(principalType)
                    .tenantId(context.getTenantId())
                    .resource(context.getResource())
                    .resourceId(context.getResourceId() != null ? context.getResourceId().toString() : null)
                    .action(context.getAction())
                    .allowed(allowed)
                    .rbacResult(rbacResult)
                    .reason(reason)
                    .requestId(context.getRequestId())
                    .build());
        } catch (Exception e) {
            log.error("Failed to log audit entry", e);
        }

        return AuthorizationDecision.builder()
                .allowed(allowed)
                .rbacResult(rbacResult)
                .scopeResult(scopeResult)
                .reason(reason)
                .matchedPolicy(matchedPolicy)
                .obligations(obligations)
                .build();
    }

    private UUID parseUserId(String sub) {
        try {
            return UUID.fromString(sub);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
