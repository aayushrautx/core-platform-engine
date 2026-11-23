package com.coreplatform.users.dto;

import java.util.UUID;

public record CreateRoleRequest(
        String name,
        String description,
        UUID parentRoleId
) {}
