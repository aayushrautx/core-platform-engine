package com.coreplatform.users.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
        log.info("=== JWT User Details ===");
        log.info("Subject: {}", jwt.getSubject());
        log.info("Username: {}", jwt.getClaimAsString("preferred_username"));
        log.info("Name: {}", jwt.getClaimAsString("name"));
        log.info("Email: {}", jwt.getClaimAsString("email"));
        log.info("Realm Access: {}", jwt.getClaimAsMap("realm_access"));
        log.info("Resource Access: {}", jwt.getClaimAsMap("resource_access"));
        log.info("Scope: {}", jwt.getClaimAsString("scope"));
        log.info("Issued At: {}", jwt.getIssuedAt());
        log.info("Expires At: {}", jwt.getExpiresAt());
        log.info("Issuer: {}", jwt.getIssuer());
        log.info("All Claims: {}", jwt.getClaims());
        log.info("========================");

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("subject", jwt.getSubject());
        info.put("preferredUsername", jwt.getClaimAsString("preferred_username"));
        info.put("name", jwt.getClaimAsString("name"));
        info.put("email", jwt.getClaimAsString("email"));
        info.put("emailVerified", jwt.getClaim("email_verified"));
        info.put("realmRoles", jwt.getClaimAsMap("realm_access"));
        info.put("clientRoles", jwt.getClaimAsMap("resource_access"));
        info.put("scope", jwt.getClaimAsString("scope"));
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
