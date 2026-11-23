package com.coreplatform.users.dto;

import java.util.UUID;

public record UpdateUserRequest(
        String username,
        String email,
        UUID orgId,
        String department,
        String designation
) {}
