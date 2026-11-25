package com.coreplatform.users.service;

import com.coreplatform.authorization.config.AuthorizationProperties;
import com.coreplatform.users.entity.UserEntity;
import com.coreplatform.users.entity.UserRoleEntity;
import com.coreplatform.users.repository.RoleRepository;
import com.coreplatform.users.repository.UserRepository;
import com.coreplatform.users.repository.UserRoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
public class UserSyncService {

    private static final String DEFAULT_ROLE_NAME = "viewer";
    private static final String SUPER_ADMIN_ROLE_NAME = "super_admin";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final TransactionTemplate transactionTemplate;
    private final AuthorizationProperties properties;

    public UserSyncService(UserRepository userRepository,
                           RoleRepository roleRepository,
                           UserRoleRepository userRoleRepository,
                           TransactionTemplate transactionTemplate,
                           AuthorizationProperties properties) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.transactionTemplate = transactionTemplate;
        this.properties = properties;
    }

    public boolean ensureUserExists(String sub, String username, String email) {
        if (userRepository.findByKeycloakSub(sub).isPresent()) {
            return false;
        }

        if (properties.getTenantMode() == AuthorizationProperties.TenantMode.MULTI_TENANT) {
            log.warn("Cannot auto-create user sub={} in multi-tenant mode", sub);
            return false;
        }

        UUID tenantId = properties.getSingleTenant().getTenantId();
        if (tenantId == null) {
            log.error("Single-tenant mode but no tenantId configured");
            return false;
        }

        try {
            transactionTemplate.executeWithoutResult(status -> {
                // Double-check inside transaction (race condition guard)
                if (userRepository.findByKeycloakSub(sub).isPresent()) {
                    return;
                }

                Instant now = Instant.now();
                UUID userId = UUID.randomUUID();

                UserEntity user = UserEntity.builder()
                        .id(userId)
                        .keycloakSub(sub)
                        .username(username != null ? username : sub)
                        .email(email != null ? email : sub + "@unknown")
                        .tenantId(tenantId)
                        .isActive(true)
                        .version(1L)
                        .createdAt(now)
                        .updatedAt(now)
                        .build();
                userRepository.save(user);

                String roleName = isBootstrapSuperAdmin(sub)
                        ? SUPER_ADMIN_ROLE_NAME
                        : DEFAULT_ROLE_NAME;

                roleRepository.findByTenantIdAndName(tenantId, roleName)
                        .ifPresent(role -> {
                            UserRoleEntity userRole = UserRoleEntity.builder()
                                    .userId(userId)
                                    .roleId(role.getId())
                                    .assignedAt(now)
                                    .build();
                            userRoleRepository.save(userRole);
                        });

                if (isBootstrapSuperAdmin(sub)) {
                    log.info("Bootstrap super-admin assigned to sub={}", sub);
                }
            });

            log.info("Auto-created user sub={} in single-tenant mode", sub);
            return true;
        } catch (Exception e) {
            if (userRepository.findByKeycloakSub(sub).isPresent()) {
                return false; // concurrent creation succeeded
            }
            log.error("Failed to auto-create user sub={}", sub, e);
            throw e;
        }
    }

    private boolean isBootstrapSuperAdmin(String sub) {
        String bootstrapSub = properties.getBootstrap().getSuperAdminSub();
        return bootstrapSub != null && !bootstrapSub.isBlank() && bootstrapSub.equals(sub);
    }
}
