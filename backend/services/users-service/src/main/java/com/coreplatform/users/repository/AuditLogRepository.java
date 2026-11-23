package com.coreplatform.users.repository;

import com.coreplatform.users.entity.AuthorizationAuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuthorizationAuditLogEntity, UUID> {
}
