package com.coreplatform.authorization.core;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class AuthorizationDecision {

    private final boolean allowed;
    private final String reason;
    private final String matchedPolicy;
    private final boolean rbacResult;
    private final String scopeResult;
    private final List<Map<String, Object>> obligations;

    public static AuthorizationDecision allow(String reason) {
        return AuthorizationDecision.builder()
                .allowed(true)
                .reason(reason)
                .build();
    }

    public static AuthorizationDecision deny(String reason) {
        return AuthorizationDecision.builder()
                .allowed(false)
                .reason(reason)
                .build();
    }
}
