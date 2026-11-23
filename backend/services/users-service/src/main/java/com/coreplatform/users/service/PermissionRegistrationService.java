package com.coreplatform.users.service;

import com.coreplatform.authorization.registry.PermissionDeclaration;
import com.coreplatform.users.entity.PermissionEntity;
import com.coreplatform.users.repository.PermissionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class PermissionRegistrationService {

    private final PermissionRepository permissionRepository;

    public PermissionRegistrationService(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Transactional
    public int register(List<PermissionDeclaration> declarations, String serviceName) {
        int registered = 0;

        for (PermissionDeclaration decl : declarations) {
            Optional<PermissionEntity> existing = permissionRepository.findByResourceAndAction(
                    decl.resource(), decl.action());

            if (existing.isPresent()) {
                PermissionEntity perm = existing.get();
                if (!perm.getServiceName().equals(serviceName) || !descriptionMatches(perm, decl)) {
                    perm.setServiceName(serviceName);
                    perm.setDescription(decl.description());
                    permissionRepository.save(perm);
                    log.debug("Updated permission {}:{} from service={}", decl.resource(), decl.action(), serviceName);
                    registered++;
                }
            } else {
                PermissionEntity perm = PermissionEntity.builder()
                        .id(UUID.randomUUID())
                        .resource(decl.resource())
                        .action(decl.action())
                        .description(decl.description())
                        .serviceName(serviceName)
                        .build();
                permissionRepository.save(perm);
                log.info("Registered new permission {}:{} from service={}", decl.resource(), decl.action(), serviceName);
                registered++;
            }
        }

        return registered;
    }

    private boolean descriptionMatches(PermissionEntity entity, PermissionDeclaration decl) {
        if (entity.getDescription() == null && decl.description() == null) return true;
        if (entity.getDescription() == null || decl.description() == null) return false;
        return entity.getDescription().equals(decl.description());
    }
}
