package com.coreplatform.users.controller;

import com.coreplatform.users.dto.CreatePermissionRequest;
import com.coreplatform.users.dto.PermissionResponse;
import com.coreplatform.users.dto.RolePermissionRequest;
import com.coreplatform.users.dto.UserResponse;
import com.coreplatform.users.service.PermissionService;
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
@RequestMapping("/api")
public class PermissionController {

    private final PermissionService permissionService;
    private final UserService userService;

    public PermissionController(PermissionService permissionService, UserService userService) {
        this.permissionService = permissionService;
        this.userService = userService;
    }

    @GetMapping("/permissions")
    public ResponseEntity<List<PermissionResponse>> listPermissions() {
        return ResponseEntity.ok(permissionService.listPermissions());
    }

    @PostMapping("/permissions")
    public ResponseEntity<PermissionResponse> createPermission(@RequestBody CreatePermissionRequest request) {
        return ResponseEntity.status(201).body(permissionService.createPermission(request));
    }

    @PostMapping("/roles/{roleId}/permissions")
    public ResponseEntity<Void> assignPermissionToRole(@PathVariable UUID roleId,
                                                        @RequestBody RolePermissionRequest request,
                                                        @AuthenticationPrincipal Jwt jwt) {
        UUID actorId = resolveActorId(jwt);
        permissionService.assignPermissionToRole(roleId, request, actorId);
        return ResponseEntity.status(201).build();
    }

    @DeleteMapping("/roles/{roleId}/permissions/{permissionId}")
    public ResponseEntity<Void> removePermissionFromRole(@PathVariable UUID roleId,
                                                          @PathVariable UUID permissionId,
                                                          @AuthenticationPrincipal Jwt jwt) {
        UUID actorId = resolveActorId(jwt);
        permissionService.removePermissionFromRole(roleId, permissionId, actorId);
        return ResponseEntity.noContent().build();
    }

    private UUID resolveActorId(Jwt jwt) {
        return userService.getUserByKeycloakSub(jwt.getSubject())
                .map(UserResponse::id)
                .orElse(null);
    }
}
