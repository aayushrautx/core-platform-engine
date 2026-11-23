package com.coreplatform.users.service;

import com.coreplatform.authorization.event.AuthorizationChangeEvent;
import com.coreplatform.authorization.event.AuthorizationChangePublisher;
import com.coreplatform.users.entity.AuthorizationOutboxEntity;
import com.coreplatform.users.repository.OutboxRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class OutboxPublisher {

    private static final Set<String> CRITICAL_EVENTS = Set.of(
            "USER_DEACTIVATED", "EMERGENCY_ACCESS_REVOKED", "SERVICE_ACCOUNT_DISABLED"
    );
    private static final int CRITICAL_MAX_RETRIES = 5;
    private static final int NORMAL_MAX_RETRIES = 1;
    private static final int ARCHIVE_DAYS = 7;

    private final OutboxRepository outboxRepository;
    private final AuthorizationChangePublisher changePublisher;
    private final Counter criticalFailedCounter;

    public OutboxPublisher(OutboxRepository outboxRepository,
                            AuthorizationChangePublisher changePublisher,
                            MeterRegistry meterRegistry) {
        this.outboxRepository = outboxRepository;
        this.changePublisher = changePublisher;
        this.criticalFailedCounter = meterRegistry.counter("authz.events.critical.failed");
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishPendingEvents() {
        List<AuthorizationOutboxEntity> pending = outboxRepository.findByPublishedFalseOrderByCreatedAtAsc();
        if (pending.isEmpty()) {
            return;
        }

        log.debug("Processing {} pending outbox events", pending.size());

        for (AuthorizationOutboxEntity entry : pending) {
            boolean isCritical = CRITICAL_EVENTS.contains(entry.getEventType());
            int maxRetries = isCritical ? CRITICAL_MAX_RETRIES : NORMAL_MAX_RETRIES;

            boolean published = attemptPublish(entry, maxRetries);

            if (published) {
                entry.setPublished(true);
                outboxRepository.save(entry);
            } else if (isCritical) {
                criticalFailedCounter.increment();
                log.error("Critical event failed after {} retries: eventType={} entityId={}",
                        maxRetries, entry.getEventType(), entry.getEntityId());
            } else {
                log.warn("Normal event failed, will be caught by reconciler: eventType={} entityId={}",
                        entry.getEventType(), entry.getEntityId());
                entry.setPublished(true);
                outboxRepository.save(entry);
            }
        }
    }

    @Scheduled(fixedDelay = 3600000)
    @Transactional
    public void archiveOldEvents() {
        Instant cutoff = Instant.now().minus(ARCHIVE_DAYS, ChronoUnit.DAYS);
        int deleted = outboxRepository.deletePublishedBefore(cutoff);
        if (deleted > 0) {
            log.info("Archived {} outbox events older than {} days", deleted, ARCHIVE_DAYS);
        }
    }

    private boolean attemptPublish(AuthorizationOutboxEntity entry, int maxRetries) {
        AuthorizationChangeEvent event = new AuthorizationChangeEvent(
                entry.getId(),
                entry.getEventType(),
                entry.getTenantId(),
                entry.getEntityType(),
                entry.getEntityId(),
                entry.getVersion(),
                entry.getEventType(),
                entry.getCreatedAt()
        );

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                changePublisher.publish(event);
                return true;
            } catch (Exception e) {
                log.warn("Publish attempt {}/{} failed for event={} entity={}: {}",
                        attempt, maxRetries, entry.getEventType(), entry.getEntityId(), e.getMessage());
            }
        }
        return false;
    }
}
