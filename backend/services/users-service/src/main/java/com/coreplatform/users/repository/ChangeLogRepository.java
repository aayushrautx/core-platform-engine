package com.coreplatform.users.repository;

import com.coreplatform.users.entity.AuthorizationChangeLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChangeLogRepository extends JpaRepository<AuthorizationChangeLogEntity, UUID> {
}
