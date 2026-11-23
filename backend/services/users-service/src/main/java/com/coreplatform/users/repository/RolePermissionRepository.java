package com.coreplatform.users.repository;

import com.coreplatform.users.entity.RolePermissionEntity;
import com.coreplatform.users.entity.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RolePermissionRepository extends JpaRepository<RolePermissionEntity, RolePermissionId> {

    List<RolePermissionEntity> findByRoleId(UUID roleId);
}
