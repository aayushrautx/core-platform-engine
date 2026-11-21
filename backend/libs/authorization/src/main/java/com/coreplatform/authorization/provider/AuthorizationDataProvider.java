package com.coreplatform.authorization.provider;

import com.coreplatform.authorization.model.Policy;
import com.coreplatform.authorization.model.Role;
import com.coreplatform.authorization.model.ServiceAccountData;
import com.coreplatform.authorization.model.UserAuthorizationData;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthorizationDataProvider {

    Optional<UserAuthorizationData> getUserData(String keycloakSub);

    Optional<ServiceAccountData> getServiceAccountData(String keycloakSub);

    Optional<Role> getRole(UUID roleId);

    List<Policy> getPolicies(UUID tenantId, String resource, String action);

    Optional<Long> getTenantAuthzVersion(UUID tenantId);
}
