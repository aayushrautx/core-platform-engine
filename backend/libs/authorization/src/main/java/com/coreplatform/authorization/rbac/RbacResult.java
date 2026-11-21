package com.coreplatform.authorization.rbac;

public record RbacResult(boolean granted, String scope) {

    public static RbacResult denied() {
        return new RbacResult(false, null);
    }

    public static RbacResult granted(String scope) {
        return new RbacResult(true, scope);
    }
}
