package com.coreplatform.users.repository;

import com.coreplatform.users.entity.PolicyHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PolicyHistoryRepository extends JpaRepository<PolicyHistoryEntity, UUID> {

    Page<PolicyHistoryEntity> findByPolicyIdOrderByChangedAtDesc(UUID policyId, Pageable pageable);

    Page<PolicyHistoryEntity> findByTenantIdOrderByChangedAtDesc(UUID tenantId, Pageable pageable);
}
