package com.coreplatform.authorization.testing;

import com.coreplatform.authorization.core.AuthorizationContext;
import com.coreplatform.authorization.core.AuthorizationDecision;
import com.coreplatform.authorization.engine.AuthorizationEngine;
import com.coreplatform.authorization.guard.TenantGuard;
import com.coreplatform.authorization.model.*;
import com.coreplatform.authorization.policy.ConditionEvaluator;
import com.coreplatform.authorization.policy.PolicyCompiler;
import com.coreplatform.authorization.policy.PolicyEvaluator;
import com.coreplatform.authorization.provider.AuthorizationDataProvider;
import com.coreplatform.authorization.provider.FallbackDataLoader;
import com.coreplatform.authorization.rbac.RbacEvaluator;
import com.coreplatform.authorization.resolver.AllScopeEvaluator;
import com.coreplatform.authorization.resolver.DepartmentScopeEvaluator;
import com.coreplatform.authorization.resolver.OrgScopeEvaluator;
import com.coreplatform.authorization.resolver.OwnScopeEvaluator;
import com.coreplatform.authorization.resolver.ScopeEnforcer;
import com.coreplatform.authorization.resolver.ScopeEvaluator;
import com.coreplatform.authorization.audit.AuditEntry;
import com.coreplatform.authorization.audit.AuditLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AuthorizationTestEngine {

    private final Map<String, UserAuthorizationData> users = new ConcurrentHashMap<>();
    private final Map<String, ServiceAccountData> serviceAccounts = new ConcurrentHashMap<>();
    private final Map<UUID, Role> roles = new ConcurrentHashMap<>();
    private final Map<String, List<Policy>> policies = new ConcurrentHashMap<>();
    private final Map<UUID, Long> tenantVersions = new ConcurrentHashMap<>();
    private final List<AuditEntry> auditLog = Collections.synchronizedList(new ArrayList<>());

    private final AuthorizationEngine engine;

    public AuthorizationTestEngine() {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        ObjectMapper objectMapper = new ObjectMapper();

        FallbackDataLoader fallbackLoader = new InMemoryDataLoader();
        AuthorizationDataProvider dataProvider = new InMemoryDataProvider();

        List<ScopeEvaluator> evaluators = List.of(
                new AllScopeEvaluator(),
                new OwnScopeEvaluator(),
                new DepartmentScopeEvaluator(),
                new OrgScopeEvaluator()
        );
        ScopeEnforcer scopeEnforcer = new ScopeEnforcer(evaluators);
        RbacEvaluator rbacEvaluator = new RbacEvaluator(dataProvider);
        ConditionEvaluator conditionEvaluator = new ConditionEvaluator();
        PolicyCompiler policyCompiler = new PolicyCompiler(objectMapper);
        PolicyEvaluator policyEvaluator = new PolicyEvaluator(dataProvider, policyCompiler, conditionEvaluator, objectMapper, meterRegistry);

        AuditLogger auditLogger = auditLog::add;

        this.engine = new AuthorizationEngine(dataProvider, new TenantGuard(), rbacEvaluator, scopeEnforcer, policyEvaluator, auditLogger, meterRegistry);
    }

    public AuthorizationTestEngine addUser(UserAuthorizationData user) {
        users.put(user.getKeycloakSub(), user);
        return this;
    }

    public AuthorizationTestEngine addServiceAccount(ServiceAccountData sa) {
        serviceAccounts.put(sa.getKeycloakSub(), sa);
        return this;
    }

    public AuthorizationTestEngine addRole(Role role) {
        roles.put(role.getId(), role);
        return this;
    }

    public AuthorizationTestEngine addPolicies(UUID tenantId, String resource, String action, List<Policy> policyList) {
        String key = tenantId + ":" + resource + ":" + action;
        policies.put(key, policyList);
        return this;
    }

    public AuthorizationTestEngine setTenantVersion(UUID tenantId, long version) {
        tenantVersions.put(tenantId, version);
        return this;
    }

    public AuthorizationDecision authorize(AuthorizationContext context) {
        return engine.authorize(context);
    }

    public List<AuditEntry> getAuditLog() {
        return Collections.unmodifiableList(auditLog);
    }

    public void clearAuditLog() {
        auditLog.clear();
    }

    public void reset() {
        users.clear();
        serviceAccounts.clear();
        roles.clear();
        policies.clear();
        tenantVersions.clear();
        auditLog.clear();
    }

    private class InMemoryDataProvider implements AuthorizationDataProvider {
        @Override
        public Optional<UserAuthorizationData> getUserData(String keycloakSub) {
            return Optional.ofNullable(users.get(keycloakSub));
        }

        @Override
        public Optional<ServiceAccountData> getServiceAccountData(String keycloakSub) {
            return Optional.ofNullable(serviceAccounts.get(keycloakSub));
        }

        @Override
        public Optional<Role> getRole(UUID roleId) {
            return Optional.ofNullable(roles.get(roleId));
        }

        @Override
        public List<Policy> getPolicies(UUID tenantId, String resource, String action) {
            String key = tenantId + ":" + resource + ":" + action;
            return policies.getOrDefault(key, Collections.emptyList());
        }

        @Override
        public Optional<Long> getTenantAuthzVersion(UUID tenantId) {
            return Optional.ofNullable(tenantVersions.get(tenantId));
        }
    }

    private class InMemoryDataLoader implements FallbackDataLoader {
        @Override
        public Optional<UserAuthorizationData> loadUserData(String keycloakSub) {
            return Optional.ofNullable(users.get(keycloakSub));
        }

        @Override
        public Optional<ServiceAccountData> loadServiceAccountData(String keycloakSub) {
            return Optional.ofNullable(serviceAccounts.get(keycloakSub));
        }

        @Override
        public Optional<Role> loadRole(UUID roleId) {
            return Optional.ofNullable(roles.get(roleId));
        }

        @Override
        public List<Policy> loadPolicies(UUID tenantId, String resource, String action) {
            String key = tenantId + ":" + resource + ":" + action;
            return policies.getOrDefault(key, Collections.emptyList());
        }

        @Override
        public Optional<Long> loadTenantAuthzVersion(UUID tenantId) {
            return Optional.ofNullable(tenantVersions.get(tenantId));
        }
    }
}
