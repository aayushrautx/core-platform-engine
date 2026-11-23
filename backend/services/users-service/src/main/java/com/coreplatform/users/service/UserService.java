package com.coreplatform.users.service;

import com.coreplatform.authorization.cache.AuthorizationRedisService;
import com.coreplatform.authorization.cache.RedisKeyBuilder;
import com.coreplatform.users.dto.UpdateUserRequest;
import com.coreplatform.users.dto.UserResponse;
import com.coreplatform.users.entity.UserEntity;
import com.coreplatform.users.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final OutboxService outboxService;
    private final ChangeLogService changeLogService;
    private final AuthorizationRedisService redisService;

    public UserService(UserRepository userRepository,
                        OutboxService outboxService,
                        ChangeLogService changeLogService,
                        AuthorizationRedisService redisService) {
        this.userRepository = userRepository;
        this.outboxService = outboxService;
        this.changeLogService = changeLogService;
        this.redisService = redisService;
    }

    public List<UserResponse> listUsers(UUID tenantId) {
        return userRepository.findByTenantId(tenantId).stream()
                .map(UserResponse::from)
                .toList();
    }

    public Optional<UserResponse> getUser(UUID userId) {
        return userRepository.findById(userId).map(UserResponse::from);
    }

    public Optional<UserResponse> getUserByKeycloakSub(String sub) {
        return userRepository.findByKeycloakSub(sub).map(UserResponse::from);
    }

    @Transactional
    public Optional<UserResponse> updateUser(UUID userId, UpdateUserRequest request, UUID actorId) {
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        UserEntity user = userOpt.get();
        UserResponse oldState = UserResponse.from(user);

        if (request.username() != null) user.setUsername(request.username());
        if (request.email() != null) user.setEmail(request.email());
        if (request.orgId() != null) user.setOrgId(request.orgId());
        if (request.department() != null) user.setDepartment(request.department());
        if (request.designation() != null) user.setDesignation(request.designation());
        user.setVersion(user.getVersion() + 1);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        UserResponse newState = UserResponse.from(user);

        outboxService.writeEvent("USER_UPDATED", user.getTenantId(), "USER",
                user.getId(), user.getVersion(), newState);
        changeLogService.logChange(actorId, "USER", user.getId(), "UPDATE", oldState, newState);
        redisService.invalidate(RedisKeyBuilder.userKey(user.getKeycloakSub()),
                user.getVersion(), "{\"type\":\"USER_UPDATED\",\"userId\":\"" + user.getId() + "\"}");

        return Optional.of(newState);
    }

    @Transactional
    public Optional<UserResponse> updateUserStatus(UUID userId, boolean active, UUID actorId) {
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        UserEntity user = userOpt.get();
        UserResponse oldState = UserResponse.from(user);

        user.setActive(active);
        user.setVersion(user.getVersion() + 1);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        UserResponse newState = UserResponse.from(user);
        String eventType = active ? "USER_ACTIVATED" : "USER_DEACTIVATED";

        outboxService.writeEvent(eventType, user.getTenantId(), "USER",
                user.getId(), user.getVersion(), newState);
        changeLogService.logChange(actorId, "USER", user.getId(), eventType, oldState, newState);
        redisService.invalidate(RedisKeyBuilder.userKey(user.getKeycloakSub()),
                user.getVersion(), "{\"type\":\"" + eventType + "\",\"userId\":\"" + user.getId() + "\"}");

        return Optional.of(newState);
    }
}
