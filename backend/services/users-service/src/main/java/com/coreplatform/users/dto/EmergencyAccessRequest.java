package com.coreplatform.users.dto;

import java.util.List;
import java.util.UUID;

public record EmergencyAccessRequest(
        UUID grantedTo,
        String reason,
        List<String> permissions,
        int durationMinutes
) {}
