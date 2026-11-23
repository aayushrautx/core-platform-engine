package com.coreplatform.users.dto;

import com.coreplatform.users.entity.RoleEntity;

import java.time.Instant;
import java.util.UUID;

public record RoleResponse(
        UUID id,
        UUID tenantId,
        String name,
        String description,
        UUID parentRoleId,
        boolean isSystem,
        boolean isActive,
        long version,
        Instant createdAt
) {
    public static RoleResponse from(RoleEntity entity) {
        return new RoleResponse(
                entity.getId(),
                entity.getTenantId(),
                entity.getName(),
                entity.getDescription(),
                entity.getParentRoleId(),
                entity.isSystem(),
                entity.isActive(),
                entity.getVersion(),
                entity.getCreatedAt()
        );
    }
}
