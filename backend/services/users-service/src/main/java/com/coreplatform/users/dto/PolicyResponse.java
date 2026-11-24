package com.coreplatform.users.dto;

import java.time.Instant;
import java.util.UUID;

public record PolicyResponse(
        UUID id,
        UUID policySetId,
        String name,
        String description,
        String effect,
        boolean overrideRbac,
        String conditionsJson,
        String obligationsJson,
        int priority,
        boolean isActive,
        long version,
        Instant createdAt,
        Instant updatedAt
) {}
