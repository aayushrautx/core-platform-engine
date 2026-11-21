package com.coreplatform.authorization.rbac;

import com.coreplatform.authorization.model.Role;
import com.coreplatform.authorization.model.UserAuthorizationData;
import com.coreplatform.authorization.provider.AuthorizationDataProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.UUID;

@Slf4j
public class RbacEvaluator {

    private final AuthorizationDataProvider dataProvider;

    public RbacEvaluator(AuthorizationDataProvider dataProvider) {
        this.dataProvider = dataProvider;
    }

    public RbacResult hasPermission(UserAuthorizationData user, String resource, String action) {
        String permissionKey = resource + ":" + action;

        if (user.getEffectivePermissions() == null || !user.getEffectivePermissions().contains(permissionKey)) {
            log.debug("RBAC denied: user={} missing permission={}", user.getKeycloakSub(), permissionKey);
            return RbacResult.denied();
        }

        String scope = resolveScope(user, permissionKey);
        log.debug("RBAC granted: user={} permission={} scope={}", user.getKeycloakSub(), permissionKey, scope);
        return RbacResult.granted(scope);
    }

    private String resolveScope(UserAuthorizationData user, String permissionKey) {
        if (user.getRoleIds() == null) {
            return null;
        }
        for (UUID roleId : user.getRoleIds()) {
            Optional<Role> role = dataProvider.getRole(roleId);
            if (role.isPresent() && role.get().getScopeByPermission() != null) {
                String scope = role.get().getScopeByPermission().get(permissionKey);
                if (scope != null) {
                    return scope;
                }
            }
        }
        return null;
    }
}
