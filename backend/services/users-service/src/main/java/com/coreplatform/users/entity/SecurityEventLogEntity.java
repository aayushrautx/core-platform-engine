package com.coreplatform.users.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "security_event_log")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityEventLogEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "target_id")
    private UUID targetId;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(columnDefinition = "jsonb")
    private String details;

    @Column(name = "request_ip")
    private String requestIp;

    @Column(nullable = false)
    private Instant timestamp;
}
