package com.coreplatform.users.dto;

public record CreatePermissionRequest(
        String resource,
        String action,
        String description
) {}
