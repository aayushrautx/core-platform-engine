package com.coreplatform.authorization.core;

import com.coreplatform.authorization.model.FreshnessLevel;
import com.coreplatform.authorization.model.PrincipalType;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;

@Getter
@Builder
public class AuthorizationContext {

    private final Principal principal;
    private final UUID tenantId;
    private final String resource;
    private final String action;
    private final Object resourceId;
    private final Map<String, Object> subjectAttributes;
    private final Map<String, Object> resourceAttributes;
    private final Map<String, Object> environmentAttributes;
    private final FreshnessLevel freshness;
    @Builder.Default
    private final UUID requestId = UUID.randomUUID();

    @Getter
    @Builder
    public static class Principal {
        private final PrincipalType type;
        private final String sub;
    }
}
