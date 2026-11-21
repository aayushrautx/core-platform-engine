package com.coreplatform.authorization.provider;

import com.coreplatform.authorization.model.Policy;
import com.coreplatform.authorization.model.Role;
import com.coreplatform.authorization.model.ServiceAccountData;
import com.coreplatform.authorization.model.UserAuthorizationData;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FallbackDataLoader {

    Optional<UserAuthorizationData> loadUserData(String keycloakSub);

    Optional<ServiceAccountData> loadServiceAccountData(String keycloakSub);

    Optional<Role> loadRole(UUID roleId);

    List<Policy> loadPolicies(UUID tenantId, String resource, String action);

    Optional<Long> loadTenantAuthzVersion(UUID tenantId);
}
