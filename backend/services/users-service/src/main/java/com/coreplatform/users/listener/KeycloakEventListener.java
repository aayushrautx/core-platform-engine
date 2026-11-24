package com.coreplatform.users.listener;

import com.coreplatform.users.entity.UserEntity;
import com.coreplatform.users.repository.UserRepository;
import com.coreplatform.users.service.OutboxService;
import com.coreplatform.users.service.SecurityEventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
public class KeycloakEventListener {

    private final UserRepository userRepository;
    private final OutboxService outboxService;
    private final SecurityEventService securityEventService;

    public KeycloakEventListener(UserRepository userRepository,
                                  OutboxService outboxService,
                                  SecurityEventService securityEventService) {
        this.userRepository = userRepository;
        this.outboxService = outboxService;
        this.securityEventService = securityEventService;
    }

    @Scheduled(fixedDelayString = "${authorization.keycloak.poll-interval-ms:60000}")
    public void pollKeycloakEvents() {
        log.debug("Polling Keycloak for user lifecycle events");
    }

    @Transactional
    public void handleUserDisabled(String keycloakSub) {
        Optional<UserEntity> userOpt = userRepository.findByKeycloakSub(keycloakSub);
        if (userOpt.isEmpty()) {
            log.warn("Keycloak disable event for unknown user: {}", keycloakSub);
            return;
        }

        UserEntity user = userOpt.get();
        if (!user.isActive()) {
            return;
        }

        user.setActive(false);
        userRepository.save(user);

        outboxService.writeEvent("USER_DEACTIVATED", user.getTenantId(), "USER",
                user.getId(), user.getVersion(), null);

        securityEventService.logEvent("USER_DEACTIVATED_BY_KEYCLOAK", null, user.getId(),
                user.getTenantId(), Map.of("keycloakSub", keycloakSub), null);

        log.warn("User deactivated via Keycloak event: keycloakSub={}, userId={}",
                keycloakSub, user.getId());
    }

    @Transactional
    public void handleUserDeleted(String keycloakSub) {
        Optional<UserEntity> userOpt = userRepository.findByKeycloakSub(keycloakSub);
        if (userOpt.isEmpty()) {
            return;
        }

        UserEntity user = userOpt.get();
        user.setActive(false);
        userRepository.save(user);

        outboxService.writeEvent("USER_DELETED", user.getTenantId(), "USER",
                user.getId(), user.getVersion(), null);

        securityEventService.logEvent("USER_DELETED_BY_KEYCLOAK", null, user.getId(),
                user.getTenantId(), Map.of("keycloakSub", keycloakSub), null);

        log.warn("User marked inactive via Keycloak delete event: keycloakSub={}, userId={}",
                keycloakSub, user.getId());
    }
}
