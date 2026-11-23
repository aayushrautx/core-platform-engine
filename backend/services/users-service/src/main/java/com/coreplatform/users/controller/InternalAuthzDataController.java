package com.coreplatform.users.controller;

import com.coreplatform.authorization.config.AuthorizationProperties;
import com.coreplatform.authorization.model.Policy;
import com.coreplatform.authorization.model.Role;
import com.coreplatform.authorization.model.ServiceAccountData;
import com.coreplatform.authorization.model.UserAuthorizationData;
import com.coreplatform.authorization.provider.FallbackDataLoader;
import com.coreplatform.authorization.registry.PermissionDeclaration;
import com.coreplatform.users.service.PermissionRegistrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/internal/authz-data")
public class InternalAuthzDataController {

    private final FallbackDataLoader fallbackDataLoader;
    private final PermissionRegistrationService permissionRegistrationService;
    private final List<String> allowedClients;

    public InternalAuthzDataController(FallbackDataLoader fallbackDataLoader,
                                        PermissionRegistrationService permissionRegistrationService,
                                        AuthorizationProperties properties) {
        this.fallbackDataLoader = fallbackDataLoader;
        this.permissionRegistrationService = permissionRegistrationService;
        this.allowedClients = properties.getInternalApi().getAllowedClients();
    }

    @GetMapping("/user/{sub}")
    public ResponseEntity<UserAuthorizationData> getUserData(@PathVariable String sub,
                                                              @AuthenticationPrincipal Jwt jwt) {
        ResponseEntity<Void> authCheck = checkClientAuth(jwt);
        if (authCheck != null) {
            return ResponseEntity.status(authCheck.getStatusCode()).build();
        }

        Optional<UserAuthorizationData> data = fallbackDataLoader.loadUserData(sub);
        return data.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/role/{id}")
    public ResponseEntity<Role> getRole(@PathVariable UUID id,
                                         @AuthenticationPrincipal Jwt jwt) {
        ResponseEntity<Void> authCheck = checkClientAuth(jwt);
        if (authCheck != null) {
            return ResponseEntity.status(authCheck.getStatusCode()).build();
        }

        Optional<Role> data = fallbackDataLoader.loadRole(id);
        return data.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/service-account/{sub}")
    public ResponseEntity<ServiceAccountData> getServiceAccountData(@PathVariable String sub,
                                                                      @AuthenticationPrincipal Jwt jwt) {
        ResponseEntity<Void> authCheck = checkClientAuth(jwt);
        if (authCheck != null) {
            return ResponseEntity.status(authCheck.getStatusCode()).build();
        }

        Optional<ServiceAccountData> data = fallbackDataLoader.loadServiceAccountData(sub);
        return data.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/policies/{tenantId}/{resource}/{action}")
    public ResponseEntity<List<Policy>> getPolicies(@PathVariable UUID tenantId,
                                                     @PathVariable String resource,
                                                     @PathVariable String action,
                                                     @AuthenticationPrincipal Jwt jwt) {
        ResponseEntity<Void> authCheck = checkClientAuth(jwt);
        if (authCheck != null) {
            return ResponseEntity.status(authCheck.getStatusCode()).build();
        }

        List<Policy> policies = fallbackDataLoader.loadPolicies(tenantId, resource, action);
        return ResponseEntity.ok(policies);
    }

    @PostMapping("/register-permissions")
    public ResponseEntity<Map<String, Object>> registerPermissions(@RequestBody List<PermissionDeclaration> declarations,
                                                                     @AuthenticationPrincipal Jwt jwt) {
        ResponseEntity<Void> authCheck = checkClientAuth(jwt);
        if (authCheck != null) {
            return ResponseEntity.status(authCheck.getStatusCode()).build();
        }

        String serviceName = jwt.getClaimAsString("clientId");
        if (serviceName == null) {
            serviceName = jwt.getClaimAsString("azp");
        }

        int count = permissionRegistrationService.register(declarations, serviceName);
        log.info("Registered {} permissions from service={}", count, serviceName);

        return ResponseEntity.ok(Map.of(
                "registered", count,
                "service", serviceName != null ? serviceName : "unknown"
        ));
    }

    private ResponseEntity<Void> checkClientAuth(Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(401).build();
        }

        if (allowedClients.isEmpty()) {
            return null;
        }

        String clientId = jwt.getClaimAsString("clientId");
        if (clientId == null) {
            clientId = jwt.getClaimAsString("azp");
        }

        if (clientId == null || !allowedClients.contains(clientId)) {
            log.warn("Unauthorized internal API access from clientId={}", clientId);
            return ResponseEntity.status(403).build();
        }

        return null;
    }
}
