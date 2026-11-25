package com.coreplatform.users.service;

import com.coreplatform.authorization.cache.AuthorizationRedisService;
import com.coreplatform.authorization.cache.RedisKeyBuilder;
import com.coreplatform.users.dto.CreateRoleRequest;
import com.coreplatform.users.dto.RoleResponse;
import com.coreplatform.users.dto.UpdateRoleRequest;
import com.coreplatform.users.entity.RoleEntity;
import com.coreplatform.users.repository.RoleRepository;
import com.coreplatform.users.repository.UserRoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class RoleService {

    private static final int MAX_HIERARCHY_DEPTH = 5;

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final DelegationService delegationService;
    private final OutboxService outboxService;
    private final ChangeLogService changeLogService;
    private final AuthorizationRedisService redisService;

    public RoleService(RoleRepository roleRepository,
                        UserRoleRepository userRoleRepository,
                        DelegationService delegationService,
                        OutboxService outboxService,
                        ChangeLogService changeLogService,
                        AuthorizationRedisService redisService) {
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.delegationService = delegationService;
        this.outboxService = outboxService;
        this.changeLogService = changeLogService;
        this.redisService = redisService;
    }

    public List<RoleResponse> listRoles(UUID tenantId) {
        return roleRepository.findByTenantIdAndIsActiveTrue(tenantId).stream()
                .map(RoleResponse::from)
                .toList();
    }

    public Optional<RoleResponse> getRole(UUID roleId) {
        return roleRepository.findById(roleId).map(RoleResponse::from);
    }

    @Transactional
    public RoleResponse createRole(CreateRoleRequest request, UUID tenantId, UUID actorId) {
        if (request.parentRoleId() == null) {
            throw new IllegalArgumentException("parentRoleId is required");
        }

        if (!delegationService.canCreateRole(actorId, request.parentRoleId())) {
            throw new SecurityException("Insufficient authority to create role under this parent");
        }

        if (roleRepository.findByTenantIdAndName(tenantId, request.name()).isPresent()) {
            throw new IllegalArgumentException("Role name already exists in this tenant");
        }

        RoleEntity role = RoleEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .name(request.name())
                .description(request.description())
                .parentRoleId(request.parentRoleId())
                .isSystem(false)
                .isActive(true)
                .version(1L)
                .createdAt(Instant.now())
                .build();
        roleRepository.save(role);

        RoleResponse response = RoleResponse.from(role);

        outboxService.writeEvent("ROLE_CREATED", tenantId, "ROLE", role.getId(), role.getVersion(), response);
        changeLogService.logChange(actorId, "ROLE", role.getId(), "CREATE", null, response);
        redisService.incrementTenantVersion(tenantId);

        return response;
    }

    @Transactional
    public Optional<RoleResponse> updateRole(UUID roleId, UpdateRoleRequest request, UUID actorId) {
        Optional<RoleEntity> roleOpt = roleRepository.findById(roleId);
        if (roleOpt.isEmpty()) {
            return Optional.empty();
        }

        RoleEntity role = roleOpt.get();
        if (role.isSystem()) {
            throw new SecurityException("System roles cannot be modified");
        }

        RoleResponse oldState = RoleResponse.from(role);

        if (request.parentRoleId() != null && !request.parentRoleId().equals(role.getParentRoleId())) {
            if (!delegationService.canCreateRole(actorId, request.parentRoleId())) {
                throw new SecurityException("Insufficient authority for this parent role");
            }
            if (wouldCreateCycle(roleId, request.parentRoleId())) {
                throw new IllegalArgumentException("This change would create a cycle in the role hierarchy");
            }
            if (depthExceeded(request.parentRoleId())) {
                throw new IllegalArgumentException("Maximum role hierarchy depth exceeded");
            }
            role.setParentRoleId(request.parentRoleId());
        }

        if (request.name() != null) {
            Optional<RoleEntity> existing = roleRepository.findByTenantIdAndName(role.getTenantId(), request.name());
            if (existing.isPresent() && !existing.get().getId().equals(roleId)) {
                throw new IllegalArgumentException("Role name already exists in this tenant");
            }
            role.setName(request.name());
        }
        if (request.description() != null) role.setDescription(request.description());

        role.setVersion(role.getVersion() + 1);
        roleRepository.save(role);

        RoleResponse newState = RoleResponse.from(role);

        outboxService.writeEvent("ROLE_UPDATED", role.getTenantId(), "ROLE",
                role.getId(), role.getVersion(), newState);
        changeLogService.logChange(actorId, "ROLE", role.getId(), "UPDATE", oldState, newState);
        redisService.invalidate(RedisKeyBuilder.roleKey(role.getId()),
                role.getVersion(), "{\"type\":\"ROLE_UPDATED\",\"roleId\":\"" + role.getId() + "\"}");
        redisService.incrementTenantVersion(role.getTenantId());

        return Optional.of(newState);
    }

    @Transactional
    public Optional<RoleResponse> deleteRole(UUID roleId, UUID actorId) {
        Optional<RoleEntity> roleOpt = roleRepository.findById(roleId);
        if (roleOpt.isEmpty()) {
            return Optional.empty();
        }

        RoleEntity role = roleOpt.get();
        if (role.isSystem()) {
            throw new SecurityException("System roles cannot be deleted");
        }

        RoleResponse oldState = RoleResponse.from(role);

        role.setActive(false);
        role.setVersion(role.getVersion() + 1);
        roleRepository.save(role);

        RoleResponse newState = RoleResponse.from(role);

        outboxService.writeEvent("ROLE_DELETED", role.getTenantId(), "ROLE",
                role.getId(), role.getVersion(), newState);
        changeLogService.logChange(actorId, "ROLE", role.getId(), "DELETE", oldState, newState);
        redisService.invalidate(RedisKeyBuilder.roleKey(role.getId()),
                role.getVersion(), "{\"type\":\"ROLE_DELETED\",\"roleId\":\"" + role.getId() + "\"}");
        redisService.incrementTenantVersion(role.getTenantId());

        return Optional.of(newState);
    }

    private boolean wouldCreateCycle(UUID roleId, UUID newParentId) {
        Set<UUID> visited = new HashSet<>();
        visited.add(roleId);
        UUID current = newParentId;
        int depth = 0;

        while (current != null && depth < MAX_HIERARCHY_DEPTH) {
            if (!visited.add(current)) {
                return true;
            }
            Optional<RoleEntity> parentOpt = roleRepository.findById(current);
            if (parentOpt.isEmpty()) break;
            current = parentOpt.get().getParentRoleId();
            depth++;
        }
        return false;
    }

    private boolean depthExceeded(UUID parentRoleId) {
        int depth = 1;
        UUID current = parentRoleId;
        Set<UUID> visited = new HashSet<>();

        while (current != null && depth <= MAX_HIERARCHY_DEPTH) {
            if (!visited.add(current)) break;
            Optional<RoleEntity> parentOpt = roleRepository.findById(current);
            if (parentOpt.isEmpty()) break;
            current = parentOpt.get().getParentRoleId();
            depth++;
        }
        return depth > MAX_HIERARCHY_DEPTH;
    }
}
