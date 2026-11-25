package com.coreplatform.users.filter;

import com.coreplatform.authorization.config.AuthorizationProperties;
import com.coreplatform.users.service.UserSyncService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
public class UserSyncFilter extends OncePerRequestFilter {

    private final UserSyncService userSyncService;
    private final AuthorizationProperties properties;

    public UserSyncFilter(UserSyncService userSyncService,
                          AuthorizationProperties properties) {
        this.userSyncService = userSyncService;
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

        if (properties.getTenantMode() == AuthorizationProperties.TenantMode.MULTI_TENANT) {
            // In multi-tenant mode, user must already exist
            filterChain.doFilter(request, response);
            return;
        }

        try {
            userSyncService.ensureUserExists(
                    sub,
                    extractUsername(jwt),
                    extractEmail(jwt)
            );
        } catch (Exception e) {
            log.error("Failed to sync user sub={}", sub, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Failed to provision user\"}");
            return;
        }

        filterChain.doFilter(request, response);
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
