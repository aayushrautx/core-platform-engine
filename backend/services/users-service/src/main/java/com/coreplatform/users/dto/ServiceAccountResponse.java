package com.coreplatform.users.dto;

import com.coreplatform.users.entity.ServiceAccountEntity;

import java.time.Instant;
import java.util.UUID;

public record ServiceAccountResponse(
        UUID id,
        String keycloakSub,
        String name,
        UUID tenantId,
        boolean isActive,
        long version,
        Instant createdAt
) {
    public static ServiceAccountResponse from(ServiceAccountEntity entity) {
        return new ServiceAccountResponse(
                entity.getId(),
                entity.getKeycloakSub(),
                entity.getName(),
                entity.getTenantId(),
                entity.isActive(),
                entity.getVersion(),
                entity.getCreatedAt()
        );
    }
}
