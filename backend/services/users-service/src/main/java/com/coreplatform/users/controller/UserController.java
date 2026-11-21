package com.coreplatform.users.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("subject", jwt.getSubject());
        info.put("preferredUsername", jwt.getClaimAsString("preferred_username"));
        info.put("name", jwt.getClaimAsString("name"));
        info.put("email", jwt.getClaimAsString("email"));
        info.put("emailVerified", jwt.getClaim("email_verified"));
        info.put("issuedAt", jwt.getIssuedAt());
        info.put("expiresAt", jwt.getExpiresAt());
        info.put("issuer", jwt.getIssuer());
        return info;
    }

    @GetMapping("/claims")
    public Map<String, Object> claims(@AuthenticationPrincipal Jwt jwt) {
        return jwt.getClaims();
    }
}
