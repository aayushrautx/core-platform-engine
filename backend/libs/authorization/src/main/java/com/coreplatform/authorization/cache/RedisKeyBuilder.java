package com.coreplatform.authorization.cache;

import java.util.UUID;

public final class RedisKeyBuilder {

    private static final String PREFIX = "authz:v1:";

    private RedisKeyBuilder() {
    }

    public static String userKey(String keycloakSub) {
        return PREFIX + "user:" + keycloakSub;
    }

    public static String roleKey(UUID roleId) {
        return PREFIX + "role:" + roleId;
    }

    public static String serviceAccountKey(String keycloakSub) {
        return PREFIX + "svc:" + keycloakSub;
    }

    public static String policyKey(UUID tenantId, String resource, String action) {
        return PREFIX + "policies:" + tenantId + ":" + resource + ":" + action;
    }

    public static String tenantVersionKey(UUID tenantId) {
        return PREFIX + "tenant-ver:" + tenantId;
    }

    public static String invalidationChannel() {
        return "authz:invalidation";
    }
}
