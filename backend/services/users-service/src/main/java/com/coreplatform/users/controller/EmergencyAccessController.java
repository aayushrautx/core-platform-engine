package com.coreplatform.users.controller;

import com.coreplatform.authorization.annotation.Authorize;
import com.coreplatform.authorization.model.FreshnessLevel;
import com.coreplatform.users.dto.EmergencyAccessRequest;
import com.coreplatform.users.dto.EmergencyAccessResponse;
import com.coreplatform.users.dto.UserResponse;
import com.coreplatform.users.service.EmergencyAccessService;
import com.coreplatform.users.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/authorization/emergency-access")
public class EmergencyAccessController {

    private final EmergencyAccessService emergencyAccessService;
    private final UserService userService;

    public EmergencyAccessController(EmergencyAccessService emergencyAccessService,
                                      UserService userService) {
        this.emergencyAccessService = emergencyAccessService;
        this.userService = userService;
    }

    @PostMapping
    @Authorize(resource = "emergency_access", action = "create", tenantIdParam = "tenantId", freshness = FreshnessLevel.HIGH)
    public ResponseEntity<EmergencyAccessResponse> grantEmergencyAccess(
            @RequestBody EmergencyAccessRequest request,
            @RequestParam UUID tenantId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID actorId = resolveActorId(jwt);
        return ResponseEntity.status(201)
                .body(emergencyAccessService.grantEmergencyAccess(request, tenantId, actorId));
    }

    @PostMapping("/{id}/revoke")
    @Authorize(resource = "emergency_access", action = "revoke", resourceIdParam = "id", freshness = FreshnessLevel.HIGH)
    public ResponseEntity<EmergencyAccessResponse> revokeEmergencyAccess(
            @PathVariable UUID id,
            @RequestParam UUID tenantId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID actorId = resolveActorId(jwt);
        return emergencyAccessService.revokeEmergencyAccess(id, actorId, tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Authorize(resource = "emergency_access", action = "read", tenantIdParam = "tenantId")
    public ResponseEntity<List<EmergencyAccessResponse>> listEmergencyAccess(@RequestParam UUID tenantId) {
        return ResponseEntity.ok(emergencyAccessService.listEmergencyAccess(tenantId));
    }

    @GetMapping("/active/{userId}")
    @Authorize(resource = "emergency_access", action = "read", tenantIdParam = "tenantId")
    public ResponseEntity<List<EmergencyAccessResponse>> getActiveEmergencyAccess(
            @PathVariable UUID userId,
            @RequestParam UUID tenantId) {
        return ResponseEntity.ok(emergencyAccessService.getActiveEmergencyAccess(tenantId, userId));
    }

    private UUID resolveActorId(Jwt jwt) {
        return userService.getUserByKeycloakSub(jwt.getSubject())
                .map(UserResponse::id)
                .orElse(null);
    }
}
