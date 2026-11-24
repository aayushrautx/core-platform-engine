package com.coreplatform.users.dto;

import java.util.Map;
import java.util.UUID;

public record SimulationRequest(
        UUID tenantId,
        String userKeycloakSub,
        String resource,
        String action,
        Map<String, Object> resourceAttributes,
        Map<String, Object> subjectAttributes,
        Map<String, Object> environmentAttributes
) {}
