package com.coreplatform.authorization.cache;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

import java.nio.charset.StandardCharsets;

@Slf4j
public class CacheInvalidationListener implements MessageListener {

    private final CaffeineAuthorizationCache caffeineCache;
    private final ObjectMapper objectMapper;

    public CacheInvalidationListener(CaffeineAuthorizationCache caffeineCache,
                                     ObjectMapper objectMapper) {
        this.caffeineCache = caffeineCache;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String payload = new String(message.getBody(), StandardCharsets.UTF_8);
            JsonNode event = objectMapper.readTree(payload);

            String entity = event.path("entity").asText();
            String id = event.path("id").asText();
            String tenantId = event.path("tenantId").asText();

            switch (entity) {
                case "user" -> caffeineCache.evict(RedisKeyBuilder.userKey(id));
                case "service_account" -> caffeineCache.evict(RedisKeyBuilder.serviceAccountKey(id));
                case "role" -> caffeineCache.evict(RedisKeyBuilder.roleKey(java.util.UUID.fromString(id)));
                case "tenant" -> caffeineCache.evictByPrefix("authz:v1:");
                case "policy" -> {
                    String resource = event.path("resource").asText();
                    String action = event.path("action").asText();
                    caffeineCache.evict(RedisKeyBuilder.policyKey(
                            java.util.UUID.fromString(tenantId), resource, action));
                }
                default -> log.warn("Unknown invalidation entity: {}", entity);
            }

            log.debug("Cache invalidation received entity={} id={}", entity, id);
        } catch (Exception e) {
            log.error("Failed to process cache invalidation message", e);
        }
    }
}
