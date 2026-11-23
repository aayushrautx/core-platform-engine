package com.coreplatform.authorization.engine;

import com.coreplatform.authorization.audit.AuditEntry;
import com.coreplatform.authorization.audit.AuditLogger;
import com.coreplatform.authorization.core.AuthorizationContext;
import com.coreplatform.authorization.core.AuthorizationDecision;
import com.coreplatform.authorization.guard.TenantGuard;
import com.coreplatform.authorization.model.PrincipalType;
import com.coreplatform.authorization.model.ServiceAccountData;
import com.coreplatform.authorization.model.UserAuthorizationData;
import com.coreplatform.authorization.provider.AuthorizationDataProvider;
import com.coreplatform.authorization.rbac.RbacEvaluator;
import com.coreplatform.authorization.rbac.RbacResult;
import com.coreplatform.authorization.resolver.ScopeEnforcer;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.UUID;

@Slf4j
public class AuthorizationEngine {

    private final AuthorizationDataProvider dataProvider;
    private final TenantGuard tenantGuard;
    private final RbacEvaluator rbacEvaluator;
    private final ScopeEnforcer scopeEnforcer;
    private final AuditLogger auditLogger;

    public AuthorizationEngine(AuthorizationDataProvider dataProvider,
                               TenantGuard tenantGuard,
                               RbacEvaluator rbacEvaluator,
                               ScopeEnforcer scopeEnforcer,
                               AuditLogger auditLogger) {
        this.dataProvider = dataProvider;
        this.tenantGuard = tenantGuard;
        this.rbacEvaluator = rbacEvaluator;
        this.scopeEnforcer = scopeEnforcer;
        this.auditLogger = auditLogger;
    }

    public AuthorizationDecision authorize(AuthorizationContext context) {
        PrincipalType principalType = context.getPrincipal().getType();

        if (principalType == PrincipalType.SERVICE_ACCOUNT) {
            return authorizeServiceAccount(context);
        }

        return authorizeUser(context);
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
        if (!rbac.granted()) {
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

        return auditAndReturn(context, PrincipalType.USER, true, true, rbac.scope(),
                "RBAC granted with scope: " + rbac.scope());
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

    private AuthorizationDecision auditAndReturn(AuthorizationContext context,
                                                  PrincipalType principalType,
                                                  boolean allowed,
                                                  boolean rbacResult,
                                                  String scopeResult,
                                                  String reason) {
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
