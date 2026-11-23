package com.coreplatform.users.service;

import com.coreplatform.users.entity.AuthorizationOutboxEntity;
import com.coreplatform.users.repository.OutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    public void writeEvent(String eventType, UUID tenantId, String entityType,
                            UUID entityId, long version, Object payload) {
        String payloadJson = null;
        if (payload != null) {
            try {
                payloadJson = objectMapper.writeValueAsString(payload);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize outbox payload for entity={}/{}", entityType, entityId, e);
            }
        }

        AuthorizationOutboxEntity entry = AuthorizationOutboxEntity.builder()
                .id(UUID.randomUUID())
                .eventType(eventType)
                .tenantId(tenantId)
                .entityType(entityType)
                .entityId(entityId)
                .version(version)
                .payload(payloadJson)
                .createdAt(Instant.now())
                .build();
        outboxRepository.save(entry);
    }
}
