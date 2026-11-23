package com.coreplatform.users.repository;

import com.coreplatform.users.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByKeycloakSub(String keycloakSub);

    List<UserEntity> findByTenantId(UUID tenantId);
}
