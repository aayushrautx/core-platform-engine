package com.coreplatform.users.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "service_account_roles")
@IdClass(ServiceAccountRoleId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceAccountRoleEntity {

    @Id
    @Column(name = "service_account_id", nullable = false)
    private UUID serviceAccountId;

    @Id
    @Column(name = "role_id", nullable = false)
    private UUID roleId;
}
