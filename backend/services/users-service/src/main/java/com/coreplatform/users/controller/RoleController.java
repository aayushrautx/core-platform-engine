package com.coreplatform.users.controller;

import com.coreplatform.authorization.annotation.Authorize;
import com.coreplatform.authorization.model.FreshnessLevel;
import com.coreplatform.users.dto.CreateRoleRequest;
import com.coreplatform.users.dto.RoleResponse;
import com.coreplatform.users.dto.UpdateRoleRequest;
import com.coreplatform.users.dto.UserResponse;
import com.coreplatform.users.service.RoleService;
import com.coreplatform.users.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;
    private final UserService userService;

    public RoleController(RoleService roleService, UserService userService) {
        this.roleService = roleService;
        this.userService = userService;
    }

    @GetMapping
    @Authorize(resource = "role", action = "read", tenantIdParam = "tenantId")
    public ResponseEntity<List<RoleResponse>> listRoles(@RequestParam UUID tenantId) {
        return ResponseEntity.ok(roleService.listRoles(tenantId));
    }

    @GetMapping("/{id}")
    @Authorize(resource = "role", action = "read", resourceIdParam = "id")
    public ResponseEntity<RoleResponse> getRole(@PathVariable UUID id) {
        return roleService.getRole(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Authorize(resource = "role", action = "create", tenantIdParam = "tenantId", freshness = FreshnessLevel.HIGH)
    public ResponseEntity<RoleResponse> createRole(@RequestBody CreateRoleRequest request,
                                                    @RequestParam UUID tenantId,
                                                    @AuthenticationPrincipal Jwt jwt) {
        UUID actorId = resolveActorId(jwt);
        RoleResponse role = roleService.createRole(request, tenantId, actorId);
        return ResponseEntity.status(201).body(role);
    }

    @PutMapping("/{id}")
    @Authorize(resource = "role", action = "update", resourceIdParam = "id", freshness = FreshnessLevel.HIGH)
    public ResponseEntity<RoleResponse> updateRole(@PathVariable UUID id,
                                                    @RequestBody UpdateRoleRequest request,
                                                    @AuthenticationPrincipal Jwt jwt) {
        UUID actorId = resolveActorId(jwt);
        return roleService.updateRole(id, request, actorId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Authorize(resource = "role", action = "delete", resourceIdParam = "id", freshness = FreshnessLevel.HIGH)
    public ResponseEntity<RoleResponse> deleteRole(@PathVariable UUID id,
                                                    @AuthenticationPrincipal Jwt jwt) {
        UUID actorId = resolveActorId(jwt);
        return roleService.deleteRole(id, actorId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private UUID resolveActorId(Jwt jwt) {
        return userService.getUserByKeycloakSub(jwt.getSubject())
                .map(UserResponse::id)
                .orElse(null);
    }
}
