package com.coreplatform.authorization.aspect;

import com.coreplatform.authorization.annotation.Authorize;
import com.coreplatform.authorization.core.AuthorizationContext;
import com.coreplatform.authorization.core.AuthorizationDecision;
import com.coreplatform.authorization.engine.AuthorizationEngine;
import com.coreplatform.authorization.model.PrincipalType;
import com.coreplatform.authorization.resolver.ResourceAttributeResolver;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Aspect
public class AuthorizeAspect {

    private static final String SERVICE_ACCOUNT_CLAIM = "clientId";

    private final AuthorizationEngine engine;
    private final Map<String, ResourceAttributeResolver> resolversByResource;
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public AuthorizeAspect(AuthorizationEngine engine, List<ResourceAttributeResolver> resolvers) {
        this.engine = engine;
        this.resolversByResource = resolvers.stream()
                .collect(Collectors.toMap(ResourceAttributeResolver::supportedResource, Function.identity()));
    }

    @Around("@annotation(authorize)")
    public Object around(ProceedingJoinPoint joinPoint, Authorize authorize) throws Throwable {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AccessDeniedException("No JWT authentication found");
        }

        String sub = jwt.getSubject();
        PrincipalType principalType = determinePrincipalType(jwt);
        UUID tenantId = extractTenantId(jwt);

        Object resourceId = extractResourceId(joinPoint, authorize);
        Map<String, Object> resourceAttributes = resolveResourceAttributes(authorize.resource(), resourceId);

        AuthorizationContext context = AuthorizationContext.builder()
                .principal(AuthorizationContext.Principal.builder()
                        .type(principalType)
                        .sub(sub)
                        .build())
                .tenantId(tenantId)
                .resource(authorize.resource())
                .action(authorize.action())
                .resourceId(resourceId)
                .resourceAttributes(resourceAttributes)
                .freshness(authorize.freshness())
                .build();

        AuthorizationDecision decision = engine.authorize(context);

        if (!decision.isAllowed()) {
            throw new AccessDeniedException(decision.getReason());
        }

        return joinPoint.proceed();
    }

    private PrincipalType determinePrincipalType(Jwt jwt) {
        if (jwt.hasClaim(SERVICE_ACCOUNT_CLAIM)) {
            return PrincipalType.SERVICE_ACCOUNT;
        }
        return PrincipalType.USER;
    }

    private UUID extractTenantId(Jwt jwt) {
        String tenantClaim = jwt.getClaimAsString("tenant_id");
        if (tenantClaim != null) {
            try {
                return UUID.fromString(tenantClaim);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid tenant_id claim in JWT: {}", tenantClaim);
            }
        }
        return null;
    }

    private Object extractResourceId(ProceedingJoinPoint joinPoint, Authorize authorize) {
        if (authorize.resourceIdParam().isEmpty()) {
            return null;
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String[] paramNames = parameterNameDiscoverer.getParameterNames(method);

        if (paramNames == null) {
            log.warn("Cannot resolve parameter names for method={}", method.getName());
            return null;
        }

        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < paramNames.length; i++) {
            if (paramNames[i].equals(authorize.resourceIdParam())) {
                return args[i];
            }
        }

        log.warn("Parameter '{}' not found in method={}", authorize.resourceIdParam(), method.getName());
        return null;
    }

    private Map<String, Object> resolveResourceAttributes(String resource, Object resourceId) {
        if (resourceId == null) {
            return Collections.emptyMap();
        }

        ResourceAttributeResolver resolver = resolversByResource.get(resource);
        if (resolver == null) {
            log.debug("No ResourceAttributeResolver for resource={}", resource);
            return Collections.emptyMap();
        }

        try {
            return resolver.resolve(resource, resourceId);
        } catch (Exception e) {
            log.error("Failed to resolve resource attributes resource={} resourceId={}", resource, resourceId, e);
            return Collections.emptyMap();
        }
    }
}
