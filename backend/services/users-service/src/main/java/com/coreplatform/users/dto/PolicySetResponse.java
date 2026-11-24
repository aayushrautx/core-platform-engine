package com.coreplatform.users.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PolicySetResponse(
        UUID id,
        UUID tenantId,
        String name,
        String description,
        String targetResource,
        String targetAction,
        String combiningStrategy,
        boolean isActive,
        long version,
        List<PolicyResponse> policies,
        Instant createdAt,
        Instant updatedAt
) {}
