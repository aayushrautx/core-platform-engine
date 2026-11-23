package com.coreplatform.users.dto;

import java.util.UUID;

public record RolePermissionRequest(
        UUID permissionId,
        String scope
) {}
