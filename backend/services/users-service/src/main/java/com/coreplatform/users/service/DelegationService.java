package com.coreplatform.users.service;

import com.coreplatform.users.entity.RoleEntity;
import com.coreplatform.users.entity.RolePermissionEntity;
import com.coreplatform.users.entity.UserRoleEntity;
import com.coreplatform.users.repository.RolePermissionRepository;
import com.coreplatform.users.repository.RoleRepository;
import com.coreplatform.users.repository.UserRoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class DelegationService {

    private static final int MAX_HIERARCHY_DEPTH = 5;

    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public DelegationService(UserRoleRepository userRoleRepository,
                              RoleRepository roleRepository,
                              RolePermissionRepository rolePermissionRepository) {
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    public boolean canAssignRole(UUID actorUserId, UUID targetRoleId, UUID targetUserId) {
        if (actorUserId.equals(targetUserId)) {
            log.warn("Self-assignment blocked for actor={}", actorUserId);
            return false;
        }

        List<RoleEntity> actorRoles = getActiveRoles(actorUserId);
        if (actorRoles.isEmpty()) {
            return false;
        }

        // Root role holders (depth 0, no parent) can assign anything
        int actorDepth = getHighestRoleDepth(actorRoles);
        if (actorDepth == 0) {
            return true;
        }

        Optional<RoleEntity> targetRoleOpt = roleRepository.findById(targetRoleId);
        if (targetRoleOpt.isEmpty()) {
            return false;
        }

        int targetDepth = getRoleDepth(targetRoleOpt.get());

        // Actor can assign roles at their depth or below (higher depth number = lower authority)
        return targetDepth >= actorDepth;
    }

    public boolean canCreateRole(UUID actorUserId, UUID parentRoleId) {
        List<RoleEntity> actorRoles = getActiveRoles(actorUserId);
        if (actorRoles.isEmpty()) {
            return false;
        }

        int actorDepth = getHighestRoleDepth(actorRoles);

        // Root role holders can create under any parent
        if (actorDepth == 0) {
            return true;
        }

        Optional<RoleEntity> parentOpt = roleRepository.findById(parentRoleId);
        if (parentOpt.isEmpty()) {
            return false;
        }

        int parentDepth = getRoleDepth(parentOpt.get());

        // Actor can create children under roles at their depth or below
        return parentDepth >= actorDepth;
    }

    public boolean canGrantPermission(UUID actorUserId, UUID permissionId) {
        Set<UUID> actorPermissionIds = getEffectivePermissionIds(actorUserId);
        return actorPermissionIds.contains(permissionId);
    }

    private List<RoleEntity> getActiveRoles(UUID userId) {
        List<UserRoleEntity> userRoles = userRoleRepository.findByUserId(userId);
        List<UUID> roleIds = userRoles.stream().map(UserRoleEntity::getRoleId).toList();

        return roleIds.stream()
                .map(roleRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(RoleEntity::isActive)
                .toList();
    }

    private int getHighestRoleDepth(List<RoleEntity> roles) {
        return roles.stream()
                .mapToInt(this::getRoleDepth)
                .min()
                .orElse(Integer.MAX_VALUE);
    }

    private int getRoleDepth(RoleEntity role) {
        int depth = 0;
        UUID parentId = role.getParentRoleId();
        Set<UUID> visited = new HashSet<>();
        visited.add(role.getId());

        while (parentId != null && depth < MAX_HIERARCHY_DEPTH) {
            if (!visited.add(parentId)) {
                break;
            }
            depth++;
            Optional<RoleEntity> parentOpt = roleRepository.findById(parentId);
            if (parentOpt.isEmpty()) {
                break;
            }
            parentId = parentOpt.get().getParentRoleId();
        }

        return depth;
    }

    private Set<UUID> getEffectivePermissionIds(UUID userId) {
        List<RoleEntity> actorRoles = getActiveRoles(userId);
        Set<UUID> permissionIds = new HashSet<>();
        Set<UUID> visitedRoles = new HashSet<>();

        for (RoleEntity role : actorRoles) {
            collectPermissionIds(role.getId(), permissionIds, visitedRoles, 0);
        }

        return permissionIds;
    }

    private void collectPermissionIds(UUID roleId, Set<UUID> permissionIds,
                                       Set<UUID> visitedRoles, int depth) {
        if (depth >= MAX_HIERARCHY_DEPTH || !visitedRoles.add(roleId)) {
            return;
        }

        Optional<RoleEntity> roleOpt = roleRepository.findById(roleId);
        if (roleOpt.isEmpty() || !roleOpt.get().isActive()) {
            return;
        }

        List<RolePermissionEntity> rolePerms = rolePermissionRepository.findByRoleId(roleId);
        for (RolePermissionEntity rp : rolePerms) {
            permissionIds.add(rp.getPermissionId());
        }

        RoleEntity role = roleOpt.get();
        if (role.getParentRoleId() != null) {
            collectPermissionIds(role.getParentRoleId(), permissionIds, visitedRoles, depth + 1);
        }
    }
}
