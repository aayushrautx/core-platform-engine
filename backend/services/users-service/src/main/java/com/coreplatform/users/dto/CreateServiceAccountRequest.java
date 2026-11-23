package com.coreplatform.users.dto;

import java.util.UUID;

public record CreateServiceAccountRequest(
        String keycloakSub,
        String name,
        UUID tenantId
) {}
