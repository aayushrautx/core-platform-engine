package com.coreplatform.authorization.aspect;

public class AccessDeniedException extends RuntimeException {

    private final String reason;

    public AccessDeniedException(String reason) {
        super("Access denied: " + reason);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
