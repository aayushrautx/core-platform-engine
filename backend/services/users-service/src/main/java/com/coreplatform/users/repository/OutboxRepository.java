package com.coreplatform.users.repository;

import com.coreplatform.users.entity.AuthorizationOutboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<AuthorizationOutboxEntity, UUID> {

    List<AuthorizationOutboxEntity> findByPublishedFalseOrderByCreatedAtAsc();
}
