package com.coreplatform.users.service;

import com.coreplatform.authorization.cache.AuthorizationRedisService;
import com.coreplatform.authorization.cache.RedisKeyBuilder;
import com.coreplatform.users.dto.CreatePermissionRequest;
import com.coreplatform.users.dto.PermissionResponse;
import com.coreplatform.users.dto.RolePermissionRequest;
import com.coreplatform.users.entity.PermissionEntity;
import com.coreplatform.users.entity.RoleEntity;
import com.coreplatform.users.entity.RolePermissionEntity;
import com.coreplatform.users.repository.PermissionRepository;
import com.coreplatform.users.repository.RolePermissionRepository;
import com.coreplatform.users.repository.RoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final RoleRepository roleRepository;
    private final DelegationService delegationService;
    private final OutboxService outboxService;
    private final ChangeLogService changeLogService;
    private final AuthorizationRedisService redisService;

    public PermissionService(PermissionRepository permissionRepository,
                              RolePermissionRepository rolePermissionRepository,
                              RoleRepository roleRepository,
                              DelegationService delegationService,
                              OutboxService outboxService,
                              ChangeLogService changeLogService,
                              AuthorizationRedisService redisService) {
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.roleRepository = roleRepository;
        this.delegationService = delegationService;
        this.outboxService = outboxService;
        this.changeLogService = changeLogService;
        this.redisService = redisService;
    }

    public List<PermissionResponse> listPermissions() {
        return permissionRepository.findAll().stream()
                .map(PermissionResponse::from)
                .toList();
    }

    @Transactional
    public PermissionResponse createPermission(CreatePermissionRequest request) {
        if (permissionRepository.findByResourceAndAction(request.resource(), request.action()).isPresent()) {
            throw new IllegalArgumentException("Permission already exists: " + request.resource() + ":" + request.action());
        }

        PermissionEntity perm = PermissionEntity.builder()
                .id(UUID.randomUUID())
                .resource(request.resource())
                .action(request.action())
                .description(request.description())
                .build();
        permissionRepository.save(perm);

        return PermissionResponse.from(perm);
    }

    @Transactional
    public void assignPermissionToRole(UUID roleId, RolePermissionRequest request, UUID actorId) {
        Optional<RoleEntity> roleOpt = roleRepository.findById(roleId);
        if (roleOpt.isEmpty()) {
            throw new IllegalArgumentException("Role not found: " + roleId);
        }

        if (!permissionRepository.existsById(request.permissionId())) {
            throw new IllegalArgumentException("Permission not found: " + request.permissionId());
        }

        if (!delegationService.canGrantPermission(actorId, request.permissionId())) {
            throw new SecurityException("Actor does not hold this permission and cannot grant it");
        }

        RolePermissionEntity rp = RolePermissionEntity.builder()
                .roleId(roleId)
                .permissionId(request.permissionId())
                .scope(request.scope() != null ? request.scope() : "all")
                .build();
        rolePermissionRepository.save(rp);

        RoleEntity role = roleOpt.get();

        outboxService.writeEvent("ROLE_PERMISSION_ADDED", role.getTenantId(), "ROLE",
                roleId, role.getVersion(), rp);
        changeLogService.logChange(actorId, "ROLE_PERMISSION", roleId,
                "ADD_PERMISSION", null, request);
        redisService.invalidate(RedisKeyBuilder.roleKey(roleId),
                role.getVersion(), "{\"type\":\"ROLE_PERMISSION_CHANGED\",\"roleId\":\"" + roleId + "\"}");
        redisService.incrementTenantVersion(role.getTenantId());
    }

    @Transactional
    public void removePermissionFromRole(UUID roleId, UUID permissionId, UUID actorId) {
        Optional<RoleEntity> roleOpt = roleRepository.findById(roleId);
        if (roleOpt.isEmpty()) {
            throw new IllegalArgumentException("Role not found: " + roleId);
        }

        rolePermissionRepository.deleteByRoleIdAndPermissionId(roleId, permissionId);

        RoleEntity role = roleOpt.get();

        outboxService.writeEvent("ROLE_PERMISSION_REMOVED", role.getTenantId(), "ROLE",
                roleId, role.getVersion(), null);
        changeLogService.logChange(actorId, "ROLE_PERMISSION", roleId,
                "REMOVE_PERMISSION", permissionId, null);
        redisService.invalidate(RedisKeyBuilder.roleKey(roleId),
                role.getVersion(), "{\"type\":\"ROLE_PERMISSION_CHANGED\",\"roleId\":\"" + roleId + "\"}");
        redisService.incrementTenantVersion(role.getTenantId());
    }
}
