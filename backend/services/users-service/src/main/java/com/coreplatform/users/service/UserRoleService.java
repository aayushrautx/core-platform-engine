package com.coreplatform.users.service;

import com.coreplatform.authorization.cache.AuthorizationRedisService;
import com.coreplatform.authorization.cache.RedisKeyBuilder;
import com.coreplatform.users.dto.RoleResponse;
import com.coreplatform.users.entity.RoleEntity;
import com.coreplatform.users.entity.UserEntity;
import com.coreplatform.users.entity.UserRoleEntity;
import com.coreplatform.users.repository.RoleRepository;
import com.coreplatform.users.repository.UserRepository;
import com.coreplatform.users.repository.UserRoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class UserRoleService {

    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DelegationService delegationService;
    private final OutboxService outboxService;
    private final ChangeLogService changeLogService;
    private final AuthorizationRedisService redisService;

    public UserRoleService(UserRoleRepository userRoleRepository,
                            UserRepository userRepository,
                            RoleRepository roleRepository,
                            DelegationService delegationService,
                            OutboxService outboxService,
                            ChangeLogService changeLogService,
                            AuthorizationRedisService redisService) {
        this.userRoleRepository = userRoleRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.delegationService = delegationService;
        this.outboxService = outboxService;
        this.changeLogService = changeLogService;
        this.redisService = redisService;
    }

    public List<RoleResponse> getUserRoles(UUID userId) {
        List<UserRoleEntity> userRoles = userRoleRepository.findByUserId(userId);
        return userRoles.stream()
                .map(ur -> roleRepository.findById(ur.getRoleId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(RoleResponse::from)
                .toList();
    }

    @Transactional
    public void assignRole(UUID userId, UUID roleId, UUID actorId) {
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found: " + userId);
        }

        if (!roleRepository.existsById(roleId)) {
            throw new IllegalArgumentException("Role not found: " + roleId);
        }

        if (!delegationService.canAssignRole(actorId, roleId, userId)) {
            throw new SecurityException("Insufficient authority to assign this role");
        }

        UserRoleEntity userRole = UserRoleEntity.builder()
                .userId(userId)
                .roleId(roleId)
                .assignedAt(Instant.now())
                .assignedBy(actorId)
                .build();
        userRoleRepository.save(userRole);

        UserEntity user = userOpt.get();
        user.setVersion(user.getVersion() + 1);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        outboxService.writeEvent("USER_ROLE_ASSIGNED", user.getTenantId(), "USER",
                userId, user.getVersion(), userRole);
        changeLogService.logChange(actorId, "USER_ROLE", userId, "ASSIGN_ROLE", null, roleId);
        redisService.invalidate(RedisKeyBuilder.userKey(user.getKeycloakSub()),
                user.getVersion(), "{\"type\":\"USER_ROLE_ASSIGNED\",\"userId\":\"" + userId + "\"}");
    }

    @Transactional
    public void removeRole(UUID userId, UUID roleId, UUID actorId) {
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found: " + userId);
        }

        userRoleRepository.deleteByUserIdAndRoleId(userId, roleId);

        UserEntity user = userOpt.get();
        user.setVersion(user.getVersion() + 1);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        outboxService.writeEvent("USER_ROLE_REMOVED", user.getTenantId(), "USER",
                userId, user.getVersion(), null);
        changeLogService.logChange(actorId, "USER_ROLE", userId, "REMOVE_ROLE", roleId, null);
        redisService.invalidate(RedisKeyBuilder.userKey(user.getKeycloakSub()),
                user.getVersion(), "{\"type\":\"USER_ROLE_REMOVED\",\"userId\":\"" + userId + "\"}");
    }
}
