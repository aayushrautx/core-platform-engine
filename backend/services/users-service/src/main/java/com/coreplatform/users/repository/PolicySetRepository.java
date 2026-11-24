package com.coreplatform.users.repository;

import com.coreplatform.users.entity.PolicySetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PolicySetRepository extends JpaRepository<PolicySetEntity, UUID> {

    @Query("SELECT DISTINCT ps FROM PolicySetEntity ps LEFT JOIN FETCH ps.policies " +
            "WHERE ps.tenantId = :tenantId AND ps.isActive = true")
    List<PolicySetEntity> findByTenantIdWithPolicies(@Param("tenantId") UUID tenantId);

    @Query("SELECT DISTINCT ps FROM PolicySetEntity ps LEFT JOIN FETCH ps.policies p " +
            "WHERE ps.tenantId = :tenantId AND ps.targetResource = :resource " +
            "AND ps.targetAction = :action AND ps.isActive = true " +
            "AND (p IS NULL OR p.isActive = true)")
    List<PolicySetEntity> findActivePolicySets(@Param("tenantId") UUID tenantId,
                                                @Param("resource") String resource,
                                                @Param("action") String action);

    Optional<PolicySetEntity> findByTenantIdAndName(UUID tenantId, String name);

    long countByTenantId(UUID tenantId);
}
