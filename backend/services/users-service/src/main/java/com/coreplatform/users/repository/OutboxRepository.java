package com.coreplatform.users.repository;

import com.coreplatform.users.entity.AuthorizationOutboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<AuthorizationOutboxEntity, UUID> {

    List<AuthorizationOutboxEntity> findByPublishedFalseOrderByCreatedAtAsc();

    @Modifying
    @Query("DELETE FROM AuthorizationOutboxEntity e WHERE e.published = true AND e.createdAt < :cutoff")
    int deletePublishedBefore(Instant cutoff);
}
