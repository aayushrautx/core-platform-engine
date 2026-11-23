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
@Table(name = "service_accounts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceAccountEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "keycloak_sub", nullable = false, unique = true, updatable = false)
    private String keycloakSub;

    @Column(nullable = false)
    private String name;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
