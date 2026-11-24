package com.coreplatform.users.controller;

import com.coreplatform.authorization.annotation.Authorize;
import com.coreplatform.authorization.model.FreshnessLevel;
import com.coreplatform.users.dto.*;
import com.coreplatform.users.service.PolicyService;
import com.coreplatform.users.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/policy-sets")
public class PolicySetController {

    private final PolicyService policyService;
    private final UserService userService;

    public PolicySetController(PolicyService policyService, UserService userService) {
        this.policyService = policyService;
        this.userService = userService;
    }

    @GetMapping
    @Authorize(resource = "policy", action = "read", tenantIdParam = "tenantId")
    public ResponseEntity<List<PolicySetResponse>> listPolicySets(@RequestParam UUID tenantId) {
        return ResponseEntity.ok(policyService.listPolicySets(tenantId));
    }

    @GetMapping("/{id}")
    @Authorize(resource = "policy", action = "read", resourceIdParam = "id")
    public ResponseEntity<PolicySetResponse> getPolicySet(@PathVariable UUID id) {
        return policyService.getPolicySet(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Authorize(resource = "policy", action = "create", tenantIdParam = "tenantId", freshness = FreshnessLevel.HIGH)
    public ResponseEntity<PolicySetResponse> createPolicySet(@RequestBody CreatePolicySetRequest request,
                                                              @RequestParam UUID tenantId,
                                                              @AuthenticationPrincipal Jwt jwt) {
        UUID actorId = resolveActorId(jwt);
        return ResponseEntity.status(201).body(policyService.createPolicySet(request, tenantId, actorId));
    }

    @PutMapping("/{id}")
    @Authorize(resource = "policy", action = "update", resourceIdParam = "id", freshness = FreshnessLevel.HIGH)
    public ResponseEntity<PolicySetResponse> updatePolicySet(@PathVariable UUID id,
                                                              @RequestBody UpdatePolicySetRequest request,
                                                              @AuthenticationPrincipal Jwt jwt) {
        UUID actorId = resolveActorId(jwt);
        return policyService.updatePolicySet(id, request, actorId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Authorize(resource = "policy", action = "delete", resourceIdParam = "id", freshness = FreshnessLevel.HIGH)
    public ResponseEntity<PolicySetResponse> deletePolicySet(@PathVariable UUID id,
                                                              @AuthenticationPrincipal Jwt jwt) {
        UUID actorId = resolveActorId(jwt);
        return policyService.deletePolicySet(id, actorId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{policySetId}/policies")
    @Authorize(resource = "policy", action = "read", resourceIdParam = "policySetId")
    public ResponseEntity<List<PolicyResponse>> listPolicies(@PathVariable UUID policySetId) {
        return ResponseEntity.ok(policyService.listPolicies(policySetId));
    }

    @GetMapping("/{policySetId}/policies/{policyId}")
    @Authorize(resource = "policy", action = "read", resourceIdParam = "policySetId")
    public ResponseEntity<PolicyResponse> getPolicy(@PathVariable UUID policySetId,
                                                     @PathVariable UUID policyId) {
        return policyService.getPolicy(policyId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{policySetId}/policies")
    @Authorize(resource = "policy", action = "create", resourceIdParam = "policySetId", freshness = FreshnessLevel.HIGH)
    public ResponseEntity<PolicyResponse> createPolicy(@PathVariable UUID policySetId,
                                                        @RequestBody CreatePolicyRequest request,
                                                        @AuthenticationPrincipal Jwt jwt) {
        UUID actorId = resolveActorId(jwt);
        return ResponseEntity.status(201).body(policyService.createPolicy(policySetId, request, actorId));
    }

    @PutMapping("/{policySetId}/policies/{policyId}")
    @Authorize(resource = "policy", action = "update", resourceIdParam = "policySetId", freshness = FreshnessLevel.HIGH)
    public ResponseEntity<PolicyResponse> updatePolicy(@PathVariable UUID policySetId,
                                                        @PathVariable UUID policyId,
                                                        @RequestBody UpdatePolicyRequest request,
                                                        @AuthenticationPrincipal Jwt jwt) {
        UUID actorId = resolveActorId(jwt);
        return policyService.updatePolicy(policyId, request, actorId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{policySetId}/policies/{policyId}")
    @Authorize(resource = "policy", action = "delete", resourceIdParam = "policySetId", freshness = FreshnessLevel.HIGH)
    public ResponseEntity<PolicyResponse> deletePolicy(@PathVariable UUID policySetId,
                                                        @PathVariable UUID policyId,
                                                        @AuthenticationPrincipal Jwt jwt) {
        UUID actorId = resolveActorId(jwt);
        return policyService.deletePolicy(policyId, actorId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private UUID resolveActorId(Jwt jwt) {
        return userService.getUserByKeycloakSub(jwt.getSubject())
                .map(UserResponse::id)
                .orElse(null);
    }
}
