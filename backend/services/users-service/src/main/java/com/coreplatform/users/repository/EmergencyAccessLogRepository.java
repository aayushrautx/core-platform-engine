package com.coreplatform.users.repository;

import com.coreplatform.users.entity.EmergencyAccessLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface EmergencyAccessLogRepository extends JpaRepository<EmergencyAccessLogEntity, UUID> {

    List<EmergencyAccessLogEntity> findByTenantIdAndGrantedToAndRevokedAtIsNullAndExpiresAtAfter(
            UUID tenantId, UUID grantedTo, Instant now);

    List<EmergencyAccessLogEntity> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
