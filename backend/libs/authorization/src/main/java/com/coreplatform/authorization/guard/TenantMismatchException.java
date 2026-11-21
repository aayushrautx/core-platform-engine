package com.coreplatform.authorization.guard;

import java.util.UUID;

public class TenantMismatchException extends RuntimeException {

    private final UUID userTenantId;
    private final UUID requestTenantId;

    public TenantMismatchException(UUID userTenantId, UUID requestTenantId) {
        super("Tenant mismatch: user belongs to tenant " + userTenantId
                + " but request targets tenant " + requestTenantId);
        this.userTenantId = userTenantId;
        this.requestTenantId = requestTenantId;
    }

    public UUID getUserTenantId() {
        return userTenantId;
    }

    public UUID getRequestTenantId() {
        return requestTenantId;
    }
}
