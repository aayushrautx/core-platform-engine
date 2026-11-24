package com.coreplatform.users.dto;

public record CreatePolicyRequest(
        String name,
        String description,
        String effect,
        boolean overrideRbac,
        String conditionsJson,
        String obligationsJson,
        int priority
) {}
