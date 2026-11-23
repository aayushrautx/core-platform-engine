package com.coreplatform.users.dto;

import com.coreplatform.users.entity.UserEntity;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String keycloakSub,
        String username,
        String email,
        UUID tenantId,
        UUID orgId,
        String department,
        String designation,
        boolean isActive,
        long version,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserResponse from(UserEntity entity) {
        return new UserResponse(
                entity.getId(),
                entity.getKeycloakSub(),
                entity.getUsername(),
                entity.getEmail(),
                entity.getTenantId(),
                entity.getOrgId(),
                entity.getDepartment(),
                entity.getDesignation(),
                entity.isActive(),
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
