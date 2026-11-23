package com.coreplatform.users.service;

import com.coreplatform.users.entity.SecurityEventLogEntity;
import com.coreplatform.users.repository.SecurityEventLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class SecurityEventService {

    private final SecurityEventLogRepository securityEventLogRepository;
    private final ObjectMapper objectMapper;
    private final Counter securityEventCounter;

    public SecurityEventService(SecurityEventLogRepository securityEventLogRepository,
                                 ObjectMapper objectMapper,
                                 MeterRegistry meterRegistry) {
        this.securityEventLogRepository = securityEventLogRepository;
        this.objectMapper = objectMapper;
        this.securityEventCounter = meterRegistry.counter("authz.security.events");
    }

    public void logEvent(String eventType, UUID actorId, UUID targetId,
                          UUID tenantId, Map<String, Object> details, String requestIp) {
        String detailsJson = null;
        if (details != null) {
            try {
                detailsJson = objectMapper.writeValueAsString(details);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize security event details", e);
            }
        }

        SecurityEventLogEntity entry = SecurityEventLogEntity.builder()
                .id(UUID.randomUUID())
                .eventType(eventType)
                .actorId(actorId)
                .targetId(targetId)
                .tenantId(tenantId)
                .details(detailsJson)
                .requestIp(requestIp)
                .timestamp(Instant.now())
                .build();
        securityEventLogRepository.save(entry);
        securityEventCounter.increment();

        log.info("Security event: type={} actor={} target={} tenant={}",
                eventType, actorId, targetId, tenantId);
    }
}
