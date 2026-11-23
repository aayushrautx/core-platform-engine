package com.coreplatform.users.service;

import com.coreplatform.authorization.cache.AuthorizationRedisService;
import com.coreplatform.authorization.cache.RedisKeyBuilder;
import com.coreplatform.users.dto.CreateServiceAccountRequest;
import com.coreplatform.users.dto.ServiceAccountResponse;
import com.coreplatform.users.entity.ServiceAccountEntity;
import com.coreplatform.users.entity.ServiceAccountRoleEntity;
import com.coreplatform.users.repository.RoleRepository;
import com.coreplatform.users.repository.ServiceAccountRepository;
import com.coreplatform.users.repository.ServiceAccountRoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class ServiceAccountService {

    private final ServiceAccountRepository serviceAccountRepository;
    private final ServiceAccountRoleRepository serviceAccountRoleRepository;
    private final RoleRepository roleRepository;
    private final OutboxService outboxService;
    private final ChangeLogService changeLogService;
    private final AuthorizationRedisService redisService;

    public ServiceAccountService(ServiceAccountRepository serviceAccountRepository,
                                  ServiceAccountRoleRepository serviceAccountRoleRepository,
                                  RoleRepository roleRepository,
                                  OutboxService outboxService,
                                  ChangeLogService changeLogService,
                                  AuthorizationRedisService redisService) {
        this.serviceAccountRepository = serviceAccountRepository;
        this.serviceAccountRoleRepository = serviceAccountRoleRepository;
        this.roleRepository = roleRepository;
        this.outboxService = outboxService;
        this.changeLogService = changeLogService;
        this.redisService = redisService;
    }

    public List<ServiceAccountResponse> listServiceAccounts(UUID tenantId) {
        return serviceAccountRepository.findByTenantId(tenantId).stream()
                .map(ServiceAccountResponse::from)
                .toList();
    }

    @Transactional
    public ServiceAccountResponse createServiceAccount(CreateServiceAccountRequest request, UUID actorId) {
        ServiceAccountEntity sa = ServiceAccountEntity.builder()
                .id(UUID.randomUUID())
                .keycloakSub(request.keycloakSub())
                .name(request.name())
                .tenantId(request.tenantId())
                .isActive(true)
                .version(1L)
                .createdAt(Instant.now())
                .build();
        serviceAccountRepository.save(sa);

        ServiceAccountResponse response = ServiceAccountResponse.from(sa);

        outboxService.writeEvent("SERVICE_ACCOUNT_CREATED", sa.getTenantId(), "SERVICE_ACCOUNT",
                sa.getId(), sa.getVersion(), response);
        changeLogService.logChange(actorId, "SERVICE_ACCOUNT", sa.getId(), "CREATE", null, response);

        return response;
    }

    @Transactional
    public void assignRole(UUID serviceAccountId, UUID roleId, UUID actorId) {
        Optional<ServiceAccountEntity> saOpt = serviceAccountRepository.findById(serviceAccountId);
        if (saOpt.isEmpty()) {
            throw new IllegalArgumentException("Service account not found: " + serviceAccountId);
        }

        if (!roleRepository.existsById(roleId)) {
            throw new IllegalArgumentException("Role not found: " + roleId);
        }

        ServiceAccountRoleEntity saRole = ServiceAccountRoleEntity.builder()
                .serviceAccountId(serviceAccountId)
                .roleId(roleId)
                .build();
        serviceAccountRoleRepository.save(saRole);

        ServiceAccountEntity sa = saOpt.get();
        sa.setVersion(sa.getVersion() + 1);
        serviceAccountRepository.save(sa);

        outboxService.writeEvent("SERVICE_ACCOUNT_ROLE_ASSIGNED", sa.getTenantId(), "SERVICE_ACCOUNT",
                serviceAccountId, sa.getVersion(), saRole);
        changeLogService.logChange(actorId, "SERVICE_ACCOUNT_ROLE", serviceAccountId,
                "ASSIGN_ROLE", null, roleId);
        redisService.invalidate(RedisKeyBuilder.serviceAccountKey(sa.getKeycloakSub()),
                sa.getVersion(), "{\"type\":\"SA_ROLE_ASSIGNED\",\"saId\":\"" + serviceAccountId + "\"}");
    }

    @Transactional
    public void removeRole(UUID serviceAccountId, UUID roleId, UUID actorId) {
        Optional<ServiceAccountEntity> saOpt = serviceAccountRepository.findById(serviceAccountId);
        if (saOpt.isEmpty()) {
            throw new IllegalArgumentException("Service account not found: " + serviceAccountId);
        }

        serviceAccountRoleRepository.deleteByServiceAccountIdAndRoleId(serviceAccountId, roleId);

        ServiceAccountEntity sa = saOpt.get();
        sa.setVersion(sa.getVersion() + 1);
        serviceAccountRepository.save(sa);

        outboxService.writeEvent("SERVICE_ACCOUNT_ROLE_REMOVED", sa.getTenantId(), "SERVICE_ACCOUNT",
                serviceAccountId, sa.getVersion(), null);
        changeLogService.logChange(actorId, "SERVICE_ACCOUNT_ROLE", serviceAccountId,
                "REMOVE_ROLE", roleId, null);
        redisService.invalidate(RedisKeyBuilder.serviceAccountKey(sa.getKeycloakSub()),
                sa.getVersion(), "{\"type\":\"SA_ROLE_REMOVED\",\"saId\":\"" + serviceAccountId + "\"}");
    }
}
