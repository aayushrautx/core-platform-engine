package com.coreplatform.users.repository;

import com.coreplatform.users.entity.SecurityEventLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SecurityEventLogRepository extends JpaRepository<SecurityEventLogEntity, UUID> {
}
