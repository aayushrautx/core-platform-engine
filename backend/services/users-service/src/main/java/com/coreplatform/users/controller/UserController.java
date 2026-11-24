package com.coreplatform.users.controller;

import com.coreplatform.authorization.annotation.Authorize;
import com.coreplatform.authorization.model.FreshnessLevel;
import com.coreplatform.users.dto.UpdateUserRequest;
import com.coreplatform.users.dto.UserResponse;
import com.coreplatform.users.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Authorize(resource = "user", action = "read", tenantIdParam = "tenantId")
    public ResponseEntity<List<UserResponse>> listUsers(@RequestParam UUID tenantId) {
        return ResponseEntity.ok(userService.listUsers(tenantId));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        return userService.getUserByKeycloakSub(jwt.getSubject())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    @Authorize(resource = "user", action = "read", resourceIdParam = "id")
    public ResponseEntity<UserResponse> getUser(@PathVariable UUID id) {
        return userService.getUser(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @Authorize(resource = "user", action = "update", resourceIdParam = "id", freshness = FreshnessLevel.MEDIUM)
    public ResponseEntity<UserResponse> updateUser(@PathVariable UUID id,
                                                    @RequestBody UpdateUserRequest request,
                                                    @AuthenticationPrincipal Jwt jwt) {
        UUID actorId = resolveActorId(jwt);
        return userService.updateUser(id, request, actorId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/status")
    @Authorize(resource = "user", action = "delete", resourceIdParam = "id", freshness = FreshnessLevel.HIGH)
    public ResponseEntity<UserResponse> updateUserStatus(@PathVariable UUID id,
                                                          @RequestBody Map<String, Boolean> body,
                                                          @AuthenticationPrincipal Jwt jwt) {
        Boolean active = body.get("active");
        if (active == null) {
            return ResponseEntity.badRequest().build();
        }
        UUID actorId = resolveActorId(jwt);
        return userService.updateUserStatus(id, active, actorId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private UUID resolveActorId(Jwt jwt) {
        return userService.getUserByKeycloakSub(jwt.getSubject())
                .map(UserResponse::id)
                .orElse(null);
    }
}
