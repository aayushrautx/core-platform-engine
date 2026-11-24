package com.coreplatform.users.dto;

public record UpdatePolicySetRequest(
        String description,
        String combiningStrategy,
        Boolean isActive
) {}
