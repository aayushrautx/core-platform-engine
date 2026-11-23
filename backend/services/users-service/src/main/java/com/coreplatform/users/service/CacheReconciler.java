package com.coreplatform.users.service;

import com.coreplatform.authorization.cache.AuthorizationRedisService;
import com.coreplatform.authorization.cache.RedisKeyBuilder;
import com.coreplatform.authorization.model.UserAuthorizationData;
import com.coreplatform.authorization.provider.FallbackDataLoader;
import com.coreplatform.users.entity.ServiceAccountEntity;
import com.coreplatform.users.entity.UserEntity;
import com.coreplatform.users.repository.ServiceAccountRepository;
import com.coreplatform.users.repository.UserRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class CacheReconciler {

    private static final int DEACTIVATION_GRACE_DAYS = 30;

    private final UserRepository userRepository;
    private final ServiceAccountRepository serviceAccountRepository;
    private final AuthorizationRedisService redisService;
    private final FallbackDataLoader fallbackDataLoader;
    private final StringRedisTemplate redisTemplate;
    private final AtomicLong redisMemoryGauge;

    public CacheReconciler(UserRepository userRepository,
                            ServiceAccountRepository serviceAccountRepository,
                            AuthorizationRedisService redisService,
                            FallbackDataLoader fallbackDataLoader,
                            StringRedisTemplate redisTemplate,
                            MeterRegistry meterRegistry) {
        this.userRepository = userRepository;
        this.serviceAccountRepository = serviceAccountRepository;
        this.redisService = redisService;
        this.fallbackDataLoader = fallbackDataLoader;
        this.redisTemplate = redisTemplate;
        this.redisMemoryGauge = new AtomicLong(0);
        meterRegistry.gauge("authz.redis.memory_used", redisMemoryGauge);
    }

    @Scheduled(fixedDelay = 600000)
    public void reconcile() {
        log.info("Starting cache reconciliation");
        int fixed = 0;
        int cleaned = 0;

        fixed += reconcileUsers();
        fixed += reconcileServiceAccounts();
        cleaned += cleanDeactivatedUsers();
        cleaned += cleanDeactivatedServiceAccounts();
        monitorRedisMemory();

        log.info("Cache reconciliation complete: fixed={} cleaned={}", fixed, cleaned);
    }

    private int reconcileUsers() {
        int fixed = 0;
        List<UserEntity> activeUsers = userRepository.findByIsActiveTrue();

        for (UserEntity user : activeUsers) {
            String key = RedisKeyBuilder.userKey(user.getKeycloakSub());
            Optional<UserAuthorizationData> cached = redisService.read(key, UserAuthorizationData.class);

            if (cached.isEmpty() || cached.get().getVersion() < user.getVersion()) {
                Optional<UserAuthorizationData> fresh = fallbackDataLoader.loadUserData(user.getKeycloakSub());
                if (fresh.isPresent()) {
                    redisService.populate(key, fresh.get(), user.getVersion());
                    fixed++;
                    log.debug("Reconciled user cache key={} dbVersion={}", key, user.getVersion());
                }
            }
        }
        return fixed;
    }

    private int reconcileServiceAccounts() {
        int fixed = 0;
        List<ServiceAccountEntity> activeSAs = serviceAccountRepository.findByIsActiveTrue();

        for (ServiceAccountEntity sa : activeSAs) {
            String key = RedisKeyBuilder.serviceAccountKey(sa.getKeycloakSub());
            var cached = redisService.read(key, com.coreplatform.authorization.model.ServiceAccountData.class);

            if (cached.isEmpty() || cached.get().getVersion() < sa.getVersion()) {
                var fresh = fallbackDataLoader.loadServiceAccountData(sa.getKeycloakSub());
                if (fresh.isPresent()) {
                    redisService.populate(key, fresh.get(), sa.getVersion());
                    fixed++;
                    log.debug("Reconciled SA cache key={} dbVersion={}", key, sa.getVersion());
                }
            }
        }
        return fixed;
    }

    private int cleanDeactivatedUsers() {
        int cleaned = 0;
        Instant cutoff = Instant.now().minus(DEACTIVATION_GRACE_DAYS, ChronoUnit.DAYS);
        List<UserEntity> deactivated = userRepository.findByIsActiveFalseAndUpdatedAtBefore(cutoff);

        for (UserEntity user : deactivated) {
            String key = RedisKeyBuilder.userKey(user.getKeycloakSub());
            Boolean deleted = redisTemplate.delete(key);
            if (Boolean.TRUE.equals(deleted)) {
                cleaned++;
                log.debug("Cleaned deactivated user cache key={}", key);
            }
        }
        return cleaned;
    }

    private int cleanDeactivatedServiceAccounts() {
        int cleaned = 0;
        List<ServiceAccountEntity> deactivated = serviceAccountRepository.findByIsActiveFalse();

        for (ServiceAccountEntity sa : deactivated) {
            String key = RedisKeyBuilder.serviceAccountKey(sa.getKeycloakSub());
            Boolean deleted = redisTemplate.delete(key);
            if (Boolean.TRUE.equals(deleted)) {
                cleaned++;
                log.debug("Cleaned deactivated SA cache key={}", key);
            }
        }
        return cleaned;
    }

    private void monitorRedisMemory() {
        try {
            Properties info = redisTemplate.getConnectionFactory()
                    .getConnection().serverCommands().info("memory");
            if (info != null) {
                String usedMemory = info.getProperty("used_memory");
                if (usedMemory != null) {
                    redisMemoryGauge.set(Long.parseLong(usedMemory));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to read Redis memory info", e);
        }
    }
}
