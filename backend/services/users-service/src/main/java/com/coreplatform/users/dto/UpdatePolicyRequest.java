package com.coreplatform.users.dto;

public record UpdatePolicyRequest(
        String description,
        String conditionsJson,
        String obligationsJson,
        Integer priority,
        Boolean isActive
) {}
