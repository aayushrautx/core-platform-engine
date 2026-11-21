package com.coreplatform.authorization.event;

import com.coreplatform.authorization.cache.AuthorizationRedisService;
import com.coreplatform.authorization.cache.RedisKeyBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class RedisChangePublisher implements AuthorizationChangePublisher {

    private final AuthorizationRedisService redisService;
    private final ObjectMapper objectMapper;

    public RedisChangePublisher(AuthorizationRedisService redisService, ObjectMapper objectMapper) {
        this.redisService = redisService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(AuthorizationChangeEvent event) {
        String cacheKey = resolveCacheKey(event);
        if (cacheKey == null) {
            log.warn("Cannot resolve cache key for event entityType={} entityId={}",
                    event.entityType(), event.entityId());
            return;
        }

        String eventPayload = serializeEvent(event);
        if (eventPayload == null) {
            return;
        }

        redisService.invalidate(cacheKey, event.version(), eventPayload);
        log.debug("Published authorization change event={} entityType={} entityId={}",
                event.eventType(), event.entityType(), event.entityId());
    }

    private String resolveCacheKey(AuthorizationChangeEvent event) {
        return switch (event.entityType()) {
            case "user" -> RedisKeyBuilder.userKey(event.entityId().toString());
            case "service_account" -> RedisKeyBuilder.serviceAccountKey(event.entityId().toString());
            case "role" -> RedisKeyBuilder.roleKey(event.entityId());
            default -> null;
        };
    }

    private String serializeEvent(AuthorizationChangeEvent event) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("entity", event.entityType());
            payload.put("id", event.entityId().toString());
            payload.put("version", event.version());
            if (event.tenantId() != null) {
                payload.put("tenantId", event.tenantId().toString());
            }
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize invalidation event", e);
            return null;
        }
    }
}
