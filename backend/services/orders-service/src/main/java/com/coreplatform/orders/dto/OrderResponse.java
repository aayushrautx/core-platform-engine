package com.coreplatform.orders.dto;

import com.coreplatform.orders.entity.OrderEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID tenantId,
        UUID ownerId,
        String ownerKeycloakSub,
        String department,
        String description,
        BigDecimal amount,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
    public static OrderResponse from(OrderEntity entity) {
        return new OrderResponse(
                entity.getId(),
                entity.getTenantId(),
                entity.getOwnerId(),
                entity.getOwnerKeycloakSub(),
                entity.getDepartment(),
                entity.getDescription(),
                entity.getAmount(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
