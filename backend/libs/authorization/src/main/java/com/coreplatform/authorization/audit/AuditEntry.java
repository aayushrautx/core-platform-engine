package com.coreplatform.authorization.audit;

import com.coreplatform.authorization.model.PrincipalType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class AuditEntry {

    private final UUID userId;
    private final PrincipalType principalType;
    private final UUID tenantId;
    private final String resource;
    private final String resourceId;
    private final String action;
    private final boolean allowed;
    private final String matchedPolicy;
    private final boolean rbacResult;
    private final String reason;
    private final String requestIp;
    @Builder.Default
    private final Instant timestamp = Instant.now();
    @Builder.Default
    private final UUID requestId = UUID.randomUUID();
}
