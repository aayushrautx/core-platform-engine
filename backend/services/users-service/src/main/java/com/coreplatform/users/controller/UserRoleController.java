package com.coreplatform.users.controller;

import com.coreplatform.users.dto.AssignRoleRequest;
import com.coreplatform.users.dto.RoleResponse;
import com.coreplatform.users.dto.UserResponse;
import com.coreplatform.users.service.UserRoleService;
import com.coreplatform.users.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users/{userId}/roles")
public class UserRoleController {

    private final UserRoleService userRoleService;
    private final UserService userService;

    public UserRoleController(UserRoleService userRoleService, UserService userService) {
        this.userRoleService = userRoleService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<RoleResponse>> getUserRoles(@PathVariable UUID userId) {
        return ResponseEntity.ok(userRoleService.getUserRoles(userId));
    }

    @PostMapping
    public ResponseEntity<Void> assignRole(@PathVariable UUID userId,
                                            @RequestBody AssignRoleRequest request,
                                            @AuthenticationPrincipal Jwt jwt) {
        UUID actorId = resolveActorId(jwt);
        userRoleService.assignRole(userId, request.roleId(), actorId);
        return ResponseEntity.status(201).build();
    }

    @DeleteMapping("/{roleId}")
    public ResponseEntity<Void> removeRole(@PathVariable UUID userId,
                                            @PathVariable UUID roleId,
                                            @AuthenticationPrincipal Jwt jwt) {
        UUID actorId = resolveActorId(jwt);
        userRoleService.removeRole(userId, roleId, actorId);
        return ResponseEntity.noContent().build();
    }

    private UUID resolveActorId(Jwt jwt) {
        return userService.getUserByKeycloakSub(jwt.getSubject())
                .map(UserResponse::id)
                .orElse(null);
    }
}
