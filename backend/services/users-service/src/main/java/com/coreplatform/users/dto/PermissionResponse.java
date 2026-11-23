package com.coreplatform.users.dto;

import com.coreplatform.users.entity.PermissionEntity;

import java.util.UUID;

public record PermissionResponse(
        UUID id,
        String resource,
        String action,
        String description,
        String serviceName
) {
    public static PermissionResponse from(PermissionEntity entity) {
        return new PermissionResponse(
                entity.getId(),
                entity.getResource(),
                entity.getAction(),
                entity.getDescription(),
                entity.getServiceName()
        );
    }
}
