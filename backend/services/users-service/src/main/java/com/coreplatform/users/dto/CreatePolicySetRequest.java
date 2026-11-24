package com.coreplatform.users.dto;

public record CreatePolicySetRequest(
        String name,
        String description,
        String targetResource,
        String targetAction,
        String combiningStrategy
) {}
