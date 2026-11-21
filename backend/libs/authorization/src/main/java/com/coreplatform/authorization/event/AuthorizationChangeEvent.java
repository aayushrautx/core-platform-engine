package com.coreplatform.authorization.event;

import java.time.Instant;
import java.util.UUID;

public record AuthorizationChangeEvent(
        UUID eventId,
        String eventType,
        UUID tenantId,
        String entityType,
        UUID entityId,
        long version,
        String action,
        Instant timestamp
) {
}
