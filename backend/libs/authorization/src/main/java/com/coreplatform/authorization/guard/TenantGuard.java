package com.coreplatform.authorization.guard;

import com.coreplatform.authorization.model.UserAuthorizationData;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public class TenantGuard {

    public void check(UserAuthorizationData user, UUID requestTenantId) {
        if (requestTenantId == null) {
            return;
        }
        if (!requestTenantId.equals(user.getTenantId())) {
            log.warn("Tenant mismatch: user={} userTenant={} requestTenant={}",
                    user.getKeycloakSub(), user.getTenantId(), requestTenantId);
            throw new TenantMismatchException(user.getTenantId(), requestTenantId);
        }
    }
}
