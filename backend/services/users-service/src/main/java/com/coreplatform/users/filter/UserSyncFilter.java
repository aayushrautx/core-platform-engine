package com.coreplatform.users.filter;

import com.coreplatform.authorization.config.AuthorizationProperties;
import com.coreplatform.users.entity.UserEntity;
import com.coreplatform.users.entity.UserRoleEntity;
import com.coreplatform.users.repository.RoleRepository;
import com.coreplatform.users.repository.UserRepository;
import com.coreplatform.users.repository.UserRoleRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Slf4j
public class UserSyncFilter extends OncePerRequestFilter {

    private static final String DEFAULT_ROLE_NAME = "viewer";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final TransactionTemplate transactionTemplate;
    private final AuthorizationProperties properties;

    public UserSyncFilter(UserRepository userRepository,
                           RoleRepository roleRepository,
                           UserRoleRepository userRoleRepository,
                           TransactionTemplate transactionTemplate,
                           AuthorizationProperties properties) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.transactionTemplate = transactionTemplate;
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            filterChain.doFilter(request, response);
            return;
        }

        String sub = jwt.getSubject();
        if (sub == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip sync for service accounts (they have clientId claim)
        String clientId = jwt.getClaimAsString("clientId");
        if (clientId != null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (userRepository.findByKeycloakSub(sub).isEmpty()) {
            if (properties.getTenantMode() == AuthorizationProperties.TenantMode.MULTI_TENANT) {
                log.warn("Unknown user sub={} in multi-tenant mode, rejecting", sub);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"User not registered. Please complete tenant registration first.\"}");
                return;
            }

            UUID tenantId = properties.getSingleTenant().getTenantId();
            if (tenantId == null) {
                log.error("Single-tenant mode but no tenantId configured");
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Server misconfiguration: no default tenant\"}");
                return;
            }

            try {
                createUserFromJwt(jwt, tenantId);
                log.info("Auto-created user sub={} in single-tenant mode", sub);
            } catch (Exception e) {
                // Another thread may have created the user concurrently
                if (userRepository.findByKeycloakSub(sub).isEmpty()) {
                    log.error("Failed to auto-create user sub={}", sub, e);
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Failed to provision user\"}");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private void createUserFromJwt(Jwt jwt, UUID tenantId) {
        transactionTemplate.executeWithoutResult(status -> {
            Instant now = Instant.now();
            UUID userId = UUID.randomUUID();

            UserEntity user = UserEntity.builder()
                    .id(userId)
                    .keycloakSub(jwt.getSubject())
                    .username(extractUsername(jwt))
                    .email(extractEmail(jwt))
                    .tenantId(tenantId)
                    .isActive(true)
                    .version(1L)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            userRepository.save(user);

            roleRepository.findByTenantIdAndName(tenantId, DEFAULT_ROLE_NAME)
                    .ifPresent(role -> {
                        UserRoleEntity userRole = UserRoleEntity.builder()
                                .userId(userId)
                                .roleId(role.getId())
                                .assignedAt(now)
                                .build();
                        userRoleRepository.save(userRole);
                    });
        });
    }

    private String extractUsername(Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        if (username != null) return username;
        String email = jwt.getClaimAsString("email");
        if (email != null) return email;
        return jwt.getSubject();
    }

    private String extractEmail(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email != null) return email;
        return jwt.getSubject() + "@unknown";
    }
}
