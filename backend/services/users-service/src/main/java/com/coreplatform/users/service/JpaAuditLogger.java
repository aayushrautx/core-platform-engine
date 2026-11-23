package com.coreplatform.users.service;

import com.coreplatform.authorization.audit.AuditEntry;
import com.coreplatform.authorization.audit.AuditLogger;
import com.coreplatform.authorization.config.AuthorizationProperties;
import com.coreplatform.users.entity.AuthorizationAuditLogEntity;
import com.coreplatform.users.repository.AuditLogRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Service
public class JpaAuditLogger implements AuditLogger {

    private final AuditLogRepository auditLogRepository;
    private final AuthorizationProperties.AuditLevel defaultLevel;
    private final SecurityEventService securityEventService;
    private final ConcurrentLinkedQueue<AuthorizationAuditLogEntity> queue = new ConcurrentLinkedQueue<>();
    private final Counter denyCounter;
    private final Counter allowCounter;

    public JpaAuditLogger(AuditLogRepository auditLogRepository,
                           AuthorizationProperties properties,
                           SecurityEventService securityEventService,
                           MeterRegistry meterRegistry) {
        this.auditLogRepository = auditLogRepository;
        this.defaultLevel = properties.getAudit().getDefaultLevel();
        this.securityEventService = securityEventService;
        this.denyCounter = meterRegistry.counter("authz.decision.result", "result", "DENY");
        this.allowCounter = meterRegistry.counter("authz.decision.result", "result", "ALLOW");
    }

    @Override
    public void log(AuditEntry entry) {
        if (entry.isAllowed()) {
            allowCounter.increment();
        } else {
            denyCounter.increment();
        }

        if (!shouldLog(entry)) {
            return;
        }

        AuthorizationAuditLogEntity entity = AuthorizationAuditLogEntity.builder()
                .id(entry.getRequestId())
                .userId(entry.getUserId())
                .tenantId(entry.getTenantId())
                .resource(entry.getResource())
                .resourceId(entry.getResourceId())
                .action(entry.getAction())
                .decision(entry.isAllowed() ? "ALLOW" : "DENY")
                .matchedPolicy(entry.getMatchedPolicy())
                .rbacResult(entry.isRbacResult())
                .reason(entry.getReason())
                .requestIp(entry.getRequestIp())
                .timestamp(entry.getTimestamp())
                .build();

        queue.add(entity);

        if (!entry.isAllowed()) {
            securityEventService.logEvent(
                    "ACCESS_DENIED",
                    entry.getUserId(),
                    null,
                    entry.getTenantId(),
                    java.util.Map.of(
                            "resource", entry.getResource(),
                            "action", entry.getAction(),
                            "reason", entry.getReason() != null ? entry.getReason() : "denied"
                    ),
                    entry.getRequestIp()
            );
        }
    }

    @Scheduled(fixedDelay = 5000)
    public void flush() {
        List<AuthorizationAuditLogEntity> batch = new ArrayList<>();
        AuthorizationAuditLogEntity entry;
        while ((entry = queue.poll()) != null) {
            batch.add(entry);
        }

        if (batch.isEmpty()) {
            return;
        }

        try {
            auditLogRepository.saveAll(batch);
            log.debug("Flushed {} audit log entries", batch.size());
        } catch (Exception e) {
            log.error("Failed to flush {} audit log entries", batch.size(), e);
            queue.addAll(batch);
        }
    }

    @PreDestroy
    public void onShutdown() {
        flush();
    }

    private boolean shouldLog(AuditEntry entry) {
        if (!entry.isAllowed()) {
            return true;
        }

        return defaultLevel == AuthorizationProperties.AuditLevel.DETAILED;
    }
}
