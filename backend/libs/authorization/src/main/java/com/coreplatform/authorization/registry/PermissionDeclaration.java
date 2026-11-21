package com.coreplatform.authorization.registry;

public record PermissionDeclaration(
        String resource,
        String action,
        String description
) {

    public static PermissionDeclaration of(String resource, String action, String description) {
        return new PermissionDeclaration(resource, action, description);
    }
}
