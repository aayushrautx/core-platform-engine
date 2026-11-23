package com.coreplatform.users.repository;

import com.coreplatform.users.entity.UserRoleEntity;
import com.coreplatform.users.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRoleEntity, UserRoleId> {

    List<UserRoleEntity> findByUserId(UUID userId);
}
