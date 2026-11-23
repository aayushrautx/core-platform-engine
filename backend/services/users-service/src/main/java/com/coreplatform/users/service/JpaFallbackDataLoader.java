package com.coreplatform.users.service;

import com.coreplatform.authorization.cache.AuthorizationRedisService;
import com.coreplatform.authorization.config.AuthorizationProperties;
import com.coreplatform.authorization.model.Role;
import com.coreplatform.authorization.model.Policy;
import com.coreplatform.authorization.model.ServiceAccountData;
import com.coreplatform.authorization.model.UserAuthorizationData;
import com.coreplatform.authorization.provider.FallbackDataLoader;
import com.coreplatform.users.entity.PermissionEntity;
import com.coreplatform.users.entity.RoleEntity;
import com.coreplatform.users.entity.RolePermissionEntity;
import com.coreplatform.users.entity.ServiceAccountEntity;
import com.coreplatform.users.entity.ServiceAccountRoleEntity;
import com.coreplatform.users.entity.UserEntity;
import com.coreplatform.users.entity.UserRoleEntity;
import com.coreplatform.users.repository.PermissionRepository;
import com.coreplatform.users.repository.RolePermissionRepository;
import com.coreplatform.users.repository.RoleRepository;
import com.coreplatform.users.repository.ServiceAccountRepository;
import com.coreplatform.users.repository.ServiceAccountRoleRepository;
import com.coreplatform.users.repository.UserRepository;
import com.coreplatform.users.repository.UserRoleRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Service
public class JpaFallbackDataLoader implements FallbackDataLoader {

    private static final int MAX_ROLE_HIERARCHY_DEPTH = 5;

    private final UserRepository userRepository;
    private final ServiceAccountRepository serviceAccountRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final ServiceAccountRoleRepository serviceAccountRoleRepository;
    private final AuthorizationRedisService redisService;
    private final Semaphore fallbackSemaphore;
    private final long queueTimeoutMs;
    private final Counter rejectedCounter;
    private final Counter fallbackRequestsCounter;
    private final ConcurrentHashMap<String, CompletableFuture<?>> inFlight = new ConcurrentHashMap<>();

    public JpaFallbackDataLoader(UserRepository userRepository,
                                  ServiceAccountRepository serviceAccountRepository,
                                  RoleRepository roleRepository,
                                  PermissionRepository permissionRepository,
                                  RolePermissionRepository rolePermissionRepository,
                                  UserRoleRepository userRoleRepository,
                                  ServiceAccountRoleRepository serviceAccountRoleRepository,
                                  AuthorizationRedisService redisService,
                                  AuthorizationProperties properties,
                                  MeterRegistry meterRegistry) {
        this.userRepository = userRepository;
        this.serviceAccountRepository = serviceAccountRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.userRoleRepository = userRoleRepository;
        this.serviceAccountRoleRepository = serviceAccountRoleRepository;
        this.redisService = redisService;
        this.fallbackSemaphore = new Semaphore(properties.getFallback().getMaxConcurrent());
        this.queueTimeoutMs = properties.getFallback().getQueueTimeout().toMillis();
        this.rejectedCounter = meterRegistry.counter("authz.fallback.rejected");
        this.fallbackRequestsCounter = meterRegistry.counter("authz.fallback.requests");
    }

    @Override
    public Optional<UserAuthorizationData> loadUserData(String keycloakSub) {
        return coalesce("user:" + keycloakSub, () -> withBudget(() -> doLoadUserData(keycloakSub)));
    }

    @Override
    public Optional<ServiceAccountData> loadServiceAccountData(String keycloakSub) {
        return coalesce("sa:" + keycloakSub, () -> withBudget(() -> doLoadServiceAccountData(keycloakSub)));
    }

    @Override
    public Optional<Role> loadRole(UUID roleId) {
        return coalesce("role:" + roleId, () -> withBudget(() -> doLoadRole(roleId)));
    }

    @Override
    public List<Policy> loadPolicies(UUID tenantId, String resource, String action) {
        return Collections.emptyList();
    }

    @Override
    public Optional<Long> loadTenantAuthzVersion(UUID tenantId) {
        return redisService.getTenantVersion(tenantId);
    }

    private Optional<UserAuthorizationData> doLoadUserData(String keycloakSub) {
        Optional<UserEntity> userOpt = userRepository.findByKeycloakSub(keycloakSub);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        UserEntity user = userOpt.get();
        List<UserRoleEntity> userRoles = userRoleRepository.findByUserId(user.getId());
        List<UUID> roleIds = userRoles.stream().map(UserRoleEntity::getRoleId).toList();

        Set<String> effectivePermissions = new HashSet<>();
        Map<String, String> scopeByPermission = new HashMap<>();
        Set<UUID> visitedRoles = new LinkedHashSet<>();

        for (UUID roleId : roleIds) {
            collectPermissionsFromHierarchy(roleId, effectivePermissions, scopeByPermission, visitedRoles, 0);
        }

        long tenantVersion = redisService.getTenantVersion(user.getTenantId())
                .orElse(0L);

        return Optional.of(UserAuthorizationData.builder()
                .userId(user.getId())
                .keycloakSub(user.getKeycloakSub())
                .tenantId(user.getTenantId())
                .orgId(user.getOrgId())
                .department(user.getDepartment())
                .designation(user.getDesignation())
                .roleIds(roleIds)
                .effectivePermissions(effectivePermissions)
                .isActive(user.isActive())
                .tenantVersion(tenantVersion)
                .version(user.getVersion())
                .build());
    }

    private void collectPermissionsFromHierarchy(UUID roleId,
                                                  Set<String> effectivePermissions,
                                                  Map<String, String> scopeByPermission,
                                                  Set<UUID> visitedRoles,
                                                  int depth) {
        if (depth >= MAX_ROLE_HIERARCHY_DEPTH || visitedRoles.contains(roleId)) {
            return;
        }
        visitedRoles.add(roleId);

        Optional<RoleEntity> roleOpt = roleRepository.findById(roleId);
        if (roleOpt.isEmpty() || !roleOpt.get().isActive()) {
            return;
        }

        RoleEntity role = roleOpt.get();
        List<RolePermissionEntity> rolePerms = rolePermissionRepository.findByRoleId(roleId);

        for (RolePermissionEntity rp : rolePerms) {
            Optional<PermissionEntity> permOpt = permissionRepository.findById(rp.getPermissionId());
            if (permOpt.isPresent()) {
                PermissionEntity perm = permOpt.get();
                String permKey = perm.getResource() + ":" + perm.getAction();
                effectivePermissions.add(permKey);
                scopeByPermission.putIfAbsent(permKey, rp.getScope());
            }
        }

        if (role.getParentRoleId() != null) {
            collectPermissionsFromHierarchy(role.getParentRoleId(), effectivePermissions, scopeByPermission, visitedRoles, depth + 1);
        }
    }

    private Optional<ServiceAccountData> doLoadServiceAccountData(String keycloakSub) {
        Optional<ServiceAccountEntity> saOpt = serviceAccountRepository.findByKeycloakSub(keycloakSub);
        if (saOpt.isEmpty()) {
            return Optional.empty();
        }

        ServiceAccountEntity sa = saOpt.get();
        List<ServiceAccountRoleEntity> saRoles = serviceAccountRoleRepository.findByServiceAccountId(sa.getId());
        List<UUID> roleIds = saRoles.stream().map(ServiceAccountRoleEntity::getRoleId).toList();

        Set<String> effectivePermissions = new HashSet<>();
        Set<UUID> visitedRoles = new LinkedHashSet<>();

        for (UUID roleId : roleIds) {
            collectServiceAccountPermissions(roleId, effectivePermissions, visitedRoles, 0);
        }

        return Optional.of(ServiceAccountData.builder()
                .id(sa.getId())
                .keycloakSub(sa.getKeycloakSub())
                .tenantId(sa.getTenantId())
                .name(sa.getName())
                .roleIds(roleIds)
                .effectivePermissions(effectivePermissions)
                .version(sa.getVersion())
                .build());
    }

    private void collectServiceAccountPermissions(UUID roleId,
                                                   Set<String> effectivePermissions,
                                                   Set<UUID> visitedRoles,
                                                   int depth) {
        if (depth >= MAX_ROLE_HIERARCHY_DEPTH || visitedRoles.contains(roleId)) {
            return;
        }
        visitedRoles.add(roleId);

        Optional<RoleEntity> roleOpt = roleRepository.findById(roleId);
        if (roleOpt.isEmpty() || !roleOpt.get().isActive()) {
            return;
        }

        RoleEntity role = roleOpt.get();
        List<RolePermissionEntity> rolePerms = rolePermissionRepository.findByRoleId(roleId);

        for (RolePermissionEntity rp : rolePerms) {
            Optional<PermissionEntity> permOpt = permissionRepository.findById(rp.getPermissionId());
            if (permOpt.isPresent()) {
                PermissionEntity perm = permOpt.get();
                effectivePermissions.add(perm.getResource() + ":" + perm.getAction());
            }
        }

        if (role.getParentRoleId() != null) {
            collectServiceAccountPermissions(role.getParentRoleId(), effectivePermissions, visitedRoles, depth + 1);
        }
    }

    private Optional<Role> doLoadRole(UUID roleId) {
        Optional<RoleEntity> roleOpt = roleRepository.findById(roleId);
        if (roleOpt.isEmpty()) {
            return Optional.empty();
        }

        RoleEntity role = roleOpt.get();
        List<RolePermissionEntity> rolePerms = rolePermissionRepository.findByRoleId(roleId);

        List<String> permissionKeys = new ArrayList<>();
        Map<String, String> scopeByPermission = new HashMap<>();

        for (RolePermissionEntity rp : rolePerms) {
            Optional<PermissionEntity> permOpt = permissionRepository.findById(rp.getPermissionId());
            if (permOpt.isPresent()) {
                PermissionEntity perm = permOpt.get();
                String permKey = perm.getResource() + ":" + perm.getAction();
                permissionKeys.add(permKey);
                scopeByPermission.put(permKey, rp.getScope());
            }
        }

        return Optional.of(Role.builder()
                .id(role.getId())
                .tenantId(role.getTenantId())
                .name(role.getName())
                .parentRoleId(role.getParentRoleId())
                .permissionKeys(permissionKeys)
                .scopeByPermission(scopeByPermission)
                .isSystem(role.isSystem())
                .isActive(role.isActive())
                .version(role.getVersion())
                .build());
    }

    private <T> T withBudget(Supplier<T> task) {
        fallbackRequestsCounter.increment();
        try {
            if (!fallbackSemaphore.tryAcquire(queueTimeoutMs, TimeUnit.MILLISECONDS)) {
                rejectedCounter.increment();
                log.warn("Fallback budget exhausted, rejecting request");
                return null;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            rejectedCounter.increment();
            return null;
        }
        try {
            return task.get();
        } finally {
            fallbackSemaphore.release();
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T coalesce(String key, Supplier<T> loader) {
        CompletableFuture<T> existing = (CompletableFuture<T>) inFlight.get(key);
        if (existing != null) {
            log.debug("Coalescing request for key={}", key);
            return existing.join();
        }

        CompletableFuture<T> future = new CompletableFuture<>();
        CompletableFuture<?> prev = inFlight.putIfAbsent(key, future);
        if (prev != null) {
            log.debug("Coalescing request for key={} (race)", key);
            return ((CompletableFuture<T>) prev).join();
        }

        try {
            T result = loader.get();
            future.complete(result);
            return result;
        } catch (Exception e) {
            future.completeExceptionally(e);
            throw e;
        } finally {
            inFlight.remove(key);
        }
    }
}
