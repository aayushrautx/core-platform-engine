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
@Table(name = "authorization_audit_log")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizationAuditLogEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(nullable = false)
    private String resource;

    @Column(name = "resource_id")
    private String resourceId;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String decision;

    @Column(name = "matched_policy")
    private String matchedPolicy;

    @Column(name = "rbac_result")
    private boolean rbacResult;

    private String reason;

    @Column(name = "request_ip")
    private String requestIp;

    @Column(nullable = false)
    private Instant timestamp;
}
