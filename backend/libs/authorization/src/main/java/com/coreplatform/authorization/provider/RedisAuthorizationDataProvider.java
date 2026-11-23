package com.coreplatform.authorization.provider;

import com.coreplatform.authorization.cache.AuthorizationRedisService;
import com.coreplatform.authorization.cache.CaffeineAuthorizationCache;
import com.coreplatform.authorization.cache.CaffeineAuthorizationCache.CacheEntry;
import com.coreplatform.authorization.cache.RedisKeyBuilder;
import com.coreplatform.authorization.model.FreshnessLevel;
import com.coreplatform.authorization.model.Policy;
import com.coreplatform.authorization.model.Role;
import com.coreplatform.authorization.model.ServiceAccountData;
import com.coreplatform.authorization.model.UserAuthorizationData;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
public class RedisAuthorizationDataProvider implements AuthorizationDataProvider {

    private static final String CIRCUIT_BREAKER_NAME = "authzRedis";
    private static final TypeReference<List<Policy>> POLICY_LIST_TYPE = new TypeReference<>() {};

    private final AuthorizationRedisService redisService;
    private final CaffeineAuthorizationCache caffeineCache;
    private final FallbackDataLoader fallbackDataLoader;
    private final ObjectMapper objectMapper;
    private final CircuitBreaker circuitBreaker;
    private final Counter fallbackCounter;
    private final Counter freshnessRereadCounter;

    public RedisAuthorizationDataProvider(AuthorizationRedisService redisService,
                                          CaffeineAuthorizationCache caffeineCache,
                                          FallbackDataLoader fallbackDataLoader,
                                          ObjectMapper objectMapper,
                                          CircuitBreakerRegistry circuitBreakerRegistry,
                                          MeterRegistry meterRegistry) {
        this.redisService = redisService;
        this.caffeineCache = caffeineCache;
        this.fallbackDataLoader = fallbackDataLoader;
        this.objectMapper = objectMapper;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME);
        this.fallbackCounter = meterRegistry.counter("authz.cache.fallback");
        this.freshnessRereadCounter = meterRegistry.counter("authz.freshness.redis_reread");
    }

    @Override
    public Optional<UserAuthorizationData> getUserData(String keycloakSub) {
        return getUserData(keycloakSub, FreshnessLevel.LOW);
    }

    public Optional<UserAuthorizationData> getUserData(String keycloakSub, FreshnessLevel freshness) {
        String key = RedisKeyBuilder.userKey(keycloakSub);

        Optional<UserAuthorizationData> cached = fromCaffeine(key, freshness);
        if (cached.isPresent()) {
            if (isTenantVersionStale(cached.get())) {
                log.debug("User data tenant-version stale, bypassing Caffeine sub={}", keycloakSub);
            } else {
                return cached;
            }
        }

        Optional<UserAuthorizationData> fromRedis = fromRedis(key, UserAuthorizationData.class);
        if (fromRedis.isPresent()) {
            caffeineCache.put(key, fromRedis.get());
            return fromRedis;
        }

        return fromFallback(key, () -> fallbackDataLoader.loadUserData(keycloakSub));
    }

    @Override
    public Optional<ServiceAccountData> getServiceAccountData(String keycloakSub) {
        return getServiceAccountData(keycloakSub, FreshnessLevel.LOW);
    }

    public Optional<ServiceAccountData> getServiceAccountData(String keycloakSub, FreshnessLevel freshness) {
        String key = RedisKeyBuilder.serviceAccountKey(keycloakSub);

        Optional<ServiceAccountData> cached = fromCaffeine(key, freshness);
        if (cached.isPresent()) {
            return cached;
        }

        Optional<ServiceAccountData> fromRedis = fromRedis(key, ServiceAccountData.class);
        if (fromRedis.isPresent()) {
            caffeineCache.put(key, fromRedis.get());
            return fromRedis;
        }

        return fromFallback(key, () -> fallbackDataLoader.loadServiceAccountData(keycloakSub));
    }

    @Override
    public Optional<Role> getRole(UUID roleId) {
        String key = RedisKeyBuilder.roleKey(roleId);

        Optional<Role> cached = fromCaffeine(key, FreshnessLevel.LOW);
        if (cached.isPresent()) {
            return cached;
        }

        Optional<Role> fromRedis = fromRedis(key, Role.class);
        if (fromRedis.isPresent()) {
            caffeineCache.put(key, fromRedis.get());
            return fromRedis;
        }

        return fromFallback(key, () -> fallbackDataLoader.loadRole(roleId));
    }

    @Override
    public List<Policy> getPolicies(UUID tenantId, String resource, String action) {
        String key = RedisKeyBuilder.policyKey(tenantId, resource, action);

        Optional<List<Policy>> cached = fromCaffeine(key, FreshnessLevel.LOW);
        if (cached.isPresent()) {
            return cached.get();
        }

        Optional<List<Policy>> fromRedis = fromRedisList(key);
        if (fromRedis.isPresent()) {
            caffeineCache.put(key, fromRedis.get());
            return fromRedis.get();
        }

        List<Policy> loaded;
        try {
            loaded = fallbackDataLoader.loadPolicies(tenantId, resource, action);
        } catch (Exception e) {
            log.error("Fallback data loader failed for policies key={}", key, e);
            return Collections.emptyList();
        }
        if (loaded == null || loaded.isEmpty()) {
            return Collections.emptyList();
        }
        populateRedisAndCaffeine(key, loaded, extractPolicyVersion(loaded));
        return loaded;
    }

    @Override
    public Optional<Long> getTenantAuthzVersion(UUID tenantId) {
        try {
            return CircuitBreaker.decorateSupplier(circuitBreaker,
                    () -> redisService.getTenantVersion(tenantId)).get();
        } catch (Exception e) {
            log.warn("Redis circuit open for tenant version, falling back tenantId={}", tenantId);
            try {
                return fallbackDataLoader.loadTenantAuthzVersion(tenantId);
            } catch (Exception fallbackEx) {
                log.error("Fallback also failed for tenant version tenantId={}", tenantId, fallbackEx);
                return Optional.empty();
            }
        }
    }

    private boolean isTenantVersionStale(UserAuthorizationData userData) {
        try {
            Optional<Long> currentVersion = getTenantAuthzVersion(userData.getTenantId());
            return currentVersion.isPresent() && userData.getTenantVersion() < currentVersion.get();
        } catch (Exception e) {
            log.warn("Failed to check tenant version, assuming fresh", e);
            return false;
        }
    }

    private <T> Optional<T> fromCaffeine(String key, FreshnessLevel freshness) {
        Optional<CacheEntry<T>> entry = caffeineCache.get(key);
        if (entry.isEmpty()) {
            return Optional.empty();
        }
        CacheEntry<T> cached = entry.get();
        if (cached.age().compareTo(freshness.getMaxStaleness()) > 0) {
            freshnessRereadCounter.increment();
            log.debug("Caffeine entry stale for freshness={} key={}", freshness, key);
            return Optional.empty();
        }
        return Optional.of(cached.getValue());
    }

    private <T> Optional<T> fromRedis(String key, Class<T> type) {
        try {
            return CircuitBreaker.decorateSupplier(circuitBreaker,
                    () -> redisService.read(key, type)).get();
        } catch (Exception e) {
            log.warn("Redis circuit open, skipping Redis for key={}", key);
            return Optional.empty();
        }
    }

    private Optional<List<Policy>> fromRedisList(String key) {
        try {
            Optional<String> raw = CircuitBreaker.decorateSupplier(circuitBreaker,
                    () -> redisService.readRaw(key)).get();
            if (raw.isEmpty()) {
                return Optional.empty();
            }
            List<Policy> policies = objectMapper.readValue(raw.get(), POLICY_LIST_TYPE);
            return Optional.of(policies);
        } catch (Exception e) {
            log.warn("Failed to read policies from Redis key={}", key, e);
            return Optional.empty();
        }
    }

    private <T> Optional<T> fromFallback(String key, java.util.function.Supplier<Optional<T>> loader) {
        fallbackCounter.increment();
        Optional<T> loaded;
        try {
            loaded = loader.get();
        } catch (Exception e) {
            log.error("Fallback data loader failed for key={}", key, e);
            return Optional.empty();
        }
        if (loaded == null || loaded.isEmpty()) {
            return Optional.empty();
        }
        T value = loaded.get();
        long version = extractVersion(value);
        populateRedisAndCaffeine(key, value, version);
        return Optional.of(value);
    }

    private void populateRedisAndCaffeine(String key, Object value, long version) {
        caffeineCache.put(key, value);
        try {
            CircuitBreaker.decorateRunnable(circuitBreaker,
                    () -> redisService.populate(key, value, version)).run();
        } catch (Exception e) {
            log.warn("Redis circuit open, skipping Redis populate for key={}", key);
        }
    }

    private long extractVersion(Object data) {
        if (data instanceof UserAuthorizationData u) return u.getVersion();
        if (data instanceof ServiceAccountData s) return s.getVersion();
        if (data instanceof Role r) return r.getVersion();
        return 0L;
    }

    private long extractPolicyVersion(List<Policy> policies) {
        return policies.stream()
                .mapToLong(Policy::getVersion)
                .max()
                .orElse(0L);
    }
}
