package com.coreplatform.gateway.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @GetMapping("/me")
    public Mono<Map<String, Object>> me(@AuthenticationPrincipal OidcUser user) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("subject", user.getSubject());
        info.put("preferredUsername", user.getPreferredUsername());
        info.put("fullName", user.getFullName());
        info.put("email", user.getEmail());
        info.put("emailVerified", user.getEmailVerified());
        info.put("issuedAt", user.getIssuedAt());
        info.put("expiresAt", user.getExpiresAt());
        info.put("issuer", user.getIssuer());
        return Mono.just(info);
    }

    @GetMapping("/claims")
    public Mono<Map<String, Object>> claims(@AuthenticationPrincipal OidcUser user) {
        return Mono.just(user.getClaims());
    }
}
