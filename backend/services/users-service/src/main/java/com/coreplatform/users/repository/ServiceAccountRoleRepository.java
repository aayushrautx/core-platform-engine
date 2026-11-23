package com.coreplatform.users.repository;

import com.coreplatform.users.entity.ServiceAccountRoleEntity;
import com.coreplatform.users.entity.ServiceAccountRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ServiceAccountRoleRepository extends JpaRepository<ServiceAccountRoleEntity, ServiceAccountRoleId> {

    List<ServiceAccountRoleEntity> findByServiceAccountId(UUID serviceAccountId);

    void deleteByServiceAccountIdAndRoleId(UUID serviceAccountId, UUID roleId);
}
