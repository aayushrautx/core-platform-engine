package com.coreplatform.gateway.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    
    @GetMapping("/me")
    public Mono<Map<String, Object>> me(@AuthenticationPrincipal OidcUser user) {
        log.info("=== OidcUser Details ===");
        log.info("Subject: {}", user.getSubject());
        log.info("Preferred Username: {}", user.getPreferredUsername());
        log.info("Full Name: {}", user.getFullName());
        log.info("Email: {}", user.getEmail());
        log.info("Email Verified: {}", user.getEmailVerified());
        log.info("Issuer: {}", user.getIssuer());
        log.info("Issued At: {}", user.getIssuedAt());
        log.info("Expires At: {}", user.getExpiresAt());
        log.info("Authorities: {}", user.getAuthorities());
        log.info("Claims: {}", user.getClaims());
        log.info("ID Token Claims: {}", user.getIdToken().getClaims());
        log.info("UserInfo Claims: {}", user.getUserInfo() != null ? user.getUserInfo().getClaims() : "null");
        log.info("UserInfo Picture: {}", user.getPicture() != null ? user.getPicture() : "null");

        log.info("=======================");
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("subject", user.getSubject());
        info.put("preferredUsername", user.getPreferredUsername());
        info.put("fullName", user.getFullName());
        info.put("email", user.getEmail());
        info.put("emailVerified", user.getEmailVerified());

        Map<String, Object> realmAccess = user.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            info.put("realmRoles", realmAccess.get("roles"));
        } else {
            info.put("realmRoles", List.of());
            info.put("_rolesNote", "realm_access not in ID token — add a mapper in Keycloak or check /api/user/access-token-claims");
        }

        Map<String, Object> resourceAccess = user.getClaimAsMap("resource_access");
        if (resourceAccess != null) {
            info.put("clientRoles", resourceAccess);
        }

        info.put("authorities", user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList());
        info.put("issuedAt", user.getIssuedAt());
        info.put("expiresAt", user.getExpiresAt());
        info.put("issuer", user.getIssuer());
        return Mono.just(info);
    }

    @GetMapping("/claims")
    public Mono<Map<String, Object>> claims(@AuthenticationPrincipal OidcUser user) {
        return Mono.just(user.getClaims());
    }

    @GetMapping("/access-token-claims")
    public Mono<Map<String, Object>> accessTokenClaims(
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient authorizedClient) {
        Map<String, Object> result = new LinkedHashMap<>();
        String tokenValue = authorizedClient.getAccessToken().getTokenValue();

        String[] parts = tokenValue.split("\\.");
        if (parts.length >= 2) {
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            try {
                var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                @SuppressWarnings("unchecked")
                Map<String, Object> claims = mapper.readValue(payload, Map.class);
                result.putAll(claims);
            } catch (Exception e) {
                result.put("error", "Failed to parse access token: " + e.getMessage());
                result.put("rawPayload", payload);
            }
        }
        return Mono.just(result);
    }

    @GetMapping("/token-info")
    public Mono<Map<String, Object>> tokenInfo(
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient authorizedClient) {
        Map<String, Object> tokenInfo = new LinkedHashMap<>();

        var accessToken = authorizedClient.getAccessToken();
        tokenInfo.put("accessTokenType", accessToken.getTokenType().getValue());
        tokenInfo.put("accessTokenScopes", accessToken.getScopes());
        tokenInfo.put("accessTokenIssuedAt", accessToken.getIssuedAt());
        tokenInfo.put("accessTokenExpiresAt", accessToken.getExpiresAt());
        tokenInfo.put("accessTokenExpired", accessToken.getExpiresAt() != null
                && Instant.now().isAfter(accessToken.getExpiresAt()));

        var refreshToken = authorizedClient.getRefreshToken();
        if (refreshToken != null) {
            tokenInfo.put("refreshTokenPresent", true);
            tokenInfo.put("refreshTokenIssuedAt", refreshToken.getIssuedAt());
            tokenInfo.put("refreshTokenExpiresAt", refreshToken.getExpiresAt());
        } else {
            tokenInfo.put("refreshTokenPresent", false);
        }

        return Mono.just(tokenInfo);
    }
}
