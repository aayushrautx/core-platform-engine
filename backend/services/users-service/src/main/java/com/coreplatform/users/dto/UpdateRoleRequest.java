package com.coreplatform.users.dto;

import java.util.UUID;

public record UpdateRoleRequest(
        String name,
        String description,
        UUID parentRoleId
) {}
