package com.coreplatform.users.controller;

import com.coreplatform.users.service.UserSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/internal/user-sync")
public class InternalUserSyncController {

    private final UserSyncService userSyncService;

    public InternalUserSyncController(UserSyncService userSyncService) {
        this.userSyncService = userSyncService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> syncUser(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(401).build();
        }

        String sub = jwt.getSubject();
        if (sub == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Missing subject claim"));
        }

        String username = jwt.getClaimAsString("preferred_username");
        if (username == null) {
            username = jwt.getClaimAsString("email");
        }
        if (username == null) {
            username = sub;
        }

        String email = jwt.getClaimAsString("email");
        if (email == null) {
            email = sub + "@unknown";
        }

        try {
            boolean created = userSyncService.ensureUserExists(sub, username, email);
            return ResponseEntity.ok(Map.of(
                    "sub", sub,
                    "created", created
            ));
        } catch (Exception e) {
            log.error("User sync failed for sub={}", sub, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to sync user"));
        }
    }
}
