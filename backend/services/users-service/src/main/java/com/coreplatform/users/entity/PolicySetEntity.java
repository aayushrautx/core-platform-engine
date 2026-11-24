package com.coreplatform.users.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "policy_sets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicySetEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "target_resource", nullable = false)
    private String targetResource;

    @Column(name = "target_action", nullable = false)
    private String targetAction;

    @Column(name = "combining_strategy", nullable = false)
    private String combiningStrategy;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(nullable = false)
    private long version;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "policySet", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PolicyEntity> policies = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (version == 0) version = 1;
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (combiningStrategy == null) combiningStrategy = "DENY_OVERRIDES";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
        version++;
    }
}
