package com.coreplatform.users.controller;

import com.coreplatform.authorization.annotation.Authorize;
import com.coreplatform.authorization.model.FreshnessLevel;
import com.coreplatform.users.dto.AssignRoleRequest;
import com.coreplatform.users.dto.CreateServiceAccountRequest;
import com.coreplatform.users.dto.ServiceAccountResponse;
import com.coreplatform.users.dto.UserResponse;
import com.coreplatform.users.service.ServiceAccountService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/service-accounts")
public class ServiceAccountController {

    private final ServiceAccountService serviceAccountService;
    private final UserService userService;

    public ServiceAccountController(ServiceAccountService serviceAccountService,
                                     UserService userService) {
        this.serviceAccountService = serviceAccountService;
        this.userService = userService;
    }

    @GetMapping
    @Authorize(resource = "service_account", action = "read", tenantIdParam = "tenantId")
    public ResponseEntity<List<ServiceAccountResponse>> listServiceAccounts(@RequestParam UUID tenantId) {
        return ResponseEntity.ok(serviceAccountService.listServiceAccounts(tenantId));
    }

    @PostMapping
    @Authorize(resource = "service_account", action = "create", freshness = FreshnessLevel.HIGH)
    public ResponseEntity<ServiceAccountResponse> createServiceAccount(@RequestBody CreateServiceAccountRequest request,
                                                                        @AuthenticationPrincipal Jwt jwt) {
        UUID actorId = resolveActorId(jwt);
        return ResponseEntity.status(201).body(serviceAccountService.createServiceAccount(request, actorId));
    }

    @PostMapping("/{id}/roles")
    @Authorize(resource = "service_account", action = "update", resourceIdParam = "id", freshness = FreshnessLevel.HIGH)
    public ResponseEntity<Void> assignRole(@PathVariable UUID id,
                                            @RequestBody AssignRoleRequest request,
                                            @AuthenticationPrincipal Jwt jwt) {
        UUID actorId = resolveActorId(jwt);
        serviceAccountService.assignRole(id, request.roleId(), actorId);
        return ResponseEntity.status(201).build();
    }

    @DeleteMapping("/{id}/roles/{roleId}")
    @Authorize(resource = "service_account", action = "update", resourceIdParam = "id", freshness = FreshnessLevel.HIGH)
    public ResponseEntity<Void> removeRole(@PathVariable UUID id,
                                            @PathVariable UUID roleId,
                                            @AuthenticationPrincipal Jwt jwt) {
        UUID actorId = resolveActorId(jwt);
        serviceAccountService.removeRole(id, roleId, actorId);
        return ResponseEntity.noContent().build();
    }

    private UUID resolveActorId(Jwt jwt) {
        return userService.getUserByKeycloakSub(jwt.getSubject())
                .map(UserResponse::id)
                .orElse(null);
    }
}
