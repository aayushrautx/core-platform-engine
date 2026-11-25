package com.coreplatform.authorization.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Slf4j
public class AuthorizationRedisService {

    private static final int TOMBSTONE_TTL_SECONDS = 5;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final DefaultRedisScript<Long> populateScript;
    private final DefaultRedisScript<Long> invalidateScript;
    private final Counter tombstoneRejectedCounter;
    private final Counter tenantVersionBumpCounter;
    private final long cacheTtlSeconds;

    public AuthorizationRedisService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper,
                                     MeterRegistry meterRegistry, long cacheTtlSeconds) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.populateScript = loadScript("lua/cache_populate.lua");
        this.invalidateScript = loadScript("lua/cache_invalidate.lua");
        this.tombstoneRejectedCounter = meterRegistry.counter("authz.cache.tombstone.rejected");
        this.tenantVersionBumpCounter = meterRegistry.counter("authz.tenant.version_bump");
        this.cacheTtlSeconds = cacheTtlSeconds;
    }

    public boolean populate(String key, Object value, long version) {
        try {
            String json = objectMapper.writeValueAsString(value);
            Long result = redisTemplate.execute(
                    populateScript,
                    Collections.singletonList(key),
                    json,
                    String.valueOf(version),
                    String.valueOf(cacheTtlSeconds)
            );
            boolean written = result != null && result == 1L;
            if (!written) {
                tombstoneRejectedCounter.increment();
                log.debug("Cache populate rejected for key={} version={} (stale)", key, version);
            }
            return written;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize cache value for key={}", key, e);
            return false;
        }
    }

    public void invalidate(String key, long version, String eventPayload) {
        redisTemplate.execute(
                invalidateScript,
                Collections.singletonList(key),
                String.valueOf(version),
                String.valueOf(TOMBSTONE_TTL_SECONDS),
                RedisKeyBuilder.invalidationChannel(),
                eventPayload
        );
        log.debug("Cache invalidated key={} version={}", key, version);
    }

    public <T> Optional<T> read(String key, Class<T> type) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return Optional.empty();
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.has("_tombstone") && node.get("_tombstone").asBoolean()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.treeToValue(node, type));
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize cache value for key={}", key, e);
            return Optional.empty();
        }
    }

    public Optional<String> readRaw(String key) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return Optional.empty();
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.has("_tombstone") && node.get("_tombstone").asBoolean()) {
                return Optional.empty();
            }
            return Optional.of(json);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse cache value for key={}", key, e);
            return Optional.empty();
        }
    }

    public long incrementTenantVersion(UUID tenantId) {
        String key = RedisKeyBuilder.tenantVersionKey(tenantId);
        Long version = redisTemplate.opsForValue().increment(key);
        if (version == null) {
            throw new IllegalStateException("Failed to increment tenant version for " + tenantId);
        }
        tenantVersionBumpCounter.increment();
        log.debug("Tenant version bumped tenantId={} version={}", tenantId, version);
        return version;
    }

    public Optional<Long> getTenantVersion(UUID tenantId) {
        String key = RedisKeyBuilder.tenantVersionKey(tenantId);
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(value));
        } catch (NumberFormatException e) {
            log.error("Invalid tenant version value for tenantId={}: {}", tenantId, value);
            return Optional.empty();
        }
    }

    private DefaultRedisScript<Long> loadScript(String path) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        try {
            byte[] bytes = new ClassPathResource(path).getInputStream().readAllBytes();
            script.setScriptText(new String(bytes, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load Lua script: " + path, e);
        }
        return script;
    }
}
