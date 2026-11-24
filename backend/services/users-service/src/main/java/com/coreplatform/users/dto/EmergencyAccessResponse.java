package com.coreplatform.users.dto;

import java.time.Instant;
import java.util.UUID;

public record EmergencyAccessResponse(
        UUID id,
        UUID tenantId,
        UUID grantedTo,
        UUID grantedBy,
        String reason,
        String permissions,
        Instant expiresAt,
        Instant revokedAt,
        Instant createdAt
) {}
