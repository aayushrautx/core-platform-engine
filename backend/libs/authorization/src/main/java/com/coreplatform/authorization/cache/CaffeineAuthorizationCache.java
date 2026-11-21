package com.coreplatform.authorization.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CaffeineAuthorizationCache {

    private final Cache<String, CacheEntry<?>> cache;

    public CaffeineAuthorizationCache(Duration ttl, long maximumSize) {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(ttl.toMillis(), TimeUnit.MILLISECONDS)
                .maximumSize(maximumSize)
                .recordStats()
                .build();
    }

    public <T> void put(String key, T value) {
        cache.put(key, new CacheEntry<>(value, Instant.now()));
        log.trace("Caffeine put key={}", key);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<CacheEntry<T>> get(String key) {
        CacheEntry<?> entry = cache.getIfPresent(key);
        if (entry == null) {
            return Optional.empty();
        }
        return Optional.of((CacheEntry<T>) entry);
    }

    public void evict(String key) {
        cache.invalidate(key);
        log.debug("Caffeine evicted key={}", key);
    }

    public void evictByPrefix(String prefix) {
        cache.asMap().keySet().removeIf(key -> key.startsWith(prefix));
        log.debug("Caffeine evicted prefix={}", prefix);
    }

    public void clear() {
        cache.invalidateAll();
        log.info("Caffeine cache cleared");
    }

    public com.github.benmanes.caffeine.cache.stats.CacheStats stats() {
        return cache.stats();
    }

    @Getter
    public static class CacheEntry<T> {
        private final T value;
        private final Instant cachedAt;

        public CacheEntry(T value, Instant cachedAt) {
            this.value = value;
            this.cachedAt = cachedAt;
        }

        public Duration age() {
            return Duration.between(cachedAt, Instant.now());
        }
    }
}
