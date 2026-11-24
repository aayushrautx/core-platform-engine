package com.coreplatform.users.service;

import com.coreplatform.users.dto.EmergencyAccessRequest;
import com.coreplatform.users.dto.EmergencyAccessResponse;
import com.coreplatform.users.entity.EmergencyAccessLogEntity;
import com.coreplatform.users.repository.EmergencyAccessLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EmergencyAccessService {

    private static final int MAX_DURATION_MINUTES = 480;

    private final EmergencyAccessLogRepository repository;
    private final SecurityEventService securityEventService;
    private final ObjectMapper objectMapper;

    public EmergencyAccessService(EmergencyAccessLogRepository repository,
                                   SecurityEventService securityEventService,
                                   ObjectMapper objectMapper) {
        this.repository = repository;
        this.securityEventService = securityEventService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public EmergencyAccessResponse grantEmergencyAccess(EmergencyAccessRequest request,
                                                         UUID tenantId,
                                                         UUID grantedBy) {
        if (request.durationMinutes() <= 0 || request.durationMinutes() > MAX_DURATION_MINUTES) {
            throw new IllegalArgumentException("Duration must be between 1 and " + MAX_DURATION_MINUTES + " minutes");
        }

        if (request.permissions() == null || request.permissions().isEmpty()) {
            throw new IllegalArgumentException("At least one permission must be specified");
        }

        String permissionsJson;
        try {
            permissionsJson = objectMapper.writeValueAsString(request.permissions());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid permissions format");
        }

        Instant expiresAt = Instant.now().plus(request.durationMinutes(), ChronoUnit.MINUTES);

        EmergencyAccessLogEntity entity = EmergencyAccessLogEntity.builder()
                .tenantId(tenantId)
                .grantedTo(request.grantedTo())
                .grantedBy(grantedBy)
                .reason(request.reason())
                .permissions(permissionsJson)
                .expiresAt(expiresAt)
                .build();

        entity = repository.save(entity);

        securityEventService.logEvent("EMERGENCY_ACCESS_GRANTED", grantedBy, request.grantedTo(),
                tenantId, java.util.Map.of(
                        "durationMinutes", request.durationMinutes(),
                        "reason", request.reason(),
                        "permissions", request.permissions()
                ), null);

        log.warn("EMERGENCY ACCESS GRANTED: tenant={}, grantedTo={}, grantedBy={}, expires={}, reason={}",
                tenantId, request.grantedTo(), grantedBy, expiresAt, request.reason());

        return toResponse(entity);
    }

    @Transactional
    public Optional<EmergencyAccessResponse> revokeEmergencyAccess(UUID id, UUID revokedBy, UUID tenantId) {
        return repository.findById(id).map(entity -> {
            if (entity.getRevokedAt() != null) {
                throw new IllegalStateException("Emergency access already revoked");
            }
            entity.setRevokedAt(Instant.now());
            entity.setRevokedBy(revokedBy);
            entity = repository.save(entity);

            securityEventService.logEvent("EMERGENCY_ACCESS_REVOKED", revokedBy, entity.getGrantedTo(),
                    tenantId, java.util.Map.of("emergencyAccessId", id.toString()), null);

            log.warn("EMERGENCY ACCESS REVOKED: id={}, revokedBy={}", id, revokedBy);
            return toResponse(entity);
        });
    }

    @Transactional(readOnly = true)
    public List<EmergencyAccessResponse> getActiveEmergencyAccess(UUID tenantId, UUID userId) {
        return repository.findByTenantIdAndGrantedToAndRevokedAtIsNullAndExpiresAtAfter(
                tenantId, userId, Instant.now()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EmergencyAccessResponse> listEmergencyAccess(UUID tenantId) {
        return repository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private EmergencyAccessResponse toResponse(EmergencyAccessLogEntity entity) {
        return new EmergencyAccessResponse(
                entity.getId(),
                entity.getTenantId(),
                entity.getGrantedTo(),
                entity.getGrantedBy(),
                entity.getReason(),
                entity.getPermissions(),
                entity.getExpiresAt(),
                entity.getRevokedAt(),
                entity.getCreatedAt()
        );
    }
}
