package com.coreplatform.users.repository;

import com.coreplatform.users.entity.ServiceAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceAccountRepository extends JpaRepository<ServiceAccountEntity, UUID> {

    Optional<ServiceAccountEntity> findByKeycloakSub(String keycloakSub);

    List<ServiceAccountEntity> findByTenantId(UUID tenantId);
}
