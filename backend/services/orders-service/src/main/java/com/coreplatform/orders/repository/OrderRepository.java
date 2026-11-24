package com.coreplatform.orders.repository;

import com.coreplatform.orders.entity.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    Page<OrderEntity> findByTenantId(UUID tenantId, Pageable pageable);

    Page<OrderEntity> findByTenantIdAndOwnerKeycloakSub(UUID tenantId, String ownerKeycloakSub, Pageable pageable);

    Page<OrderEntity> findByTenantIdAndDepartment(UUID tenantId, String department, Pageable pageable);

    Optional<OrderEntity> findByIdAndTenantId(UUID id, UUID tenantId);
}
