package com.coreplatform.users.service;

import com.coreplatform.users.entity.AuthorizationChangeLogEntity;
import com.coreplatform.users.repository.ChangeLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
public class ChangeLogService {

    private final ChangeLogRepository changeLogRepository;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public ChangeLogService(ChangeLogRepository changeLogRepository, ObjectMapper objectMapper,
                             MeterRegistry meterRegistry) {
        this.changeLogRepository = changeLogRepository;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    public void logChange(UUID actorId, String entityType, UUID entityId,
                           String action, Object oldValue, Object newValue) {
        meterRegistry.counter("authz.admin.changes", "entityType", entityType).increment();
        AuthorizationChangeLogEntity entry = AuthorizationChangeLogEntity.builder()
                .id(UUID.randomUUID())
                .actorId(actorId)
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .oldValue(serialize(oldValue))
                .newValue(serialize(newValue))
                .timestamp(Instant.now())
                .build();
        changeLogRepository.save(entry);
    }

    private String serialize(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize change log value", e);
            return null;
        }
    }
}
