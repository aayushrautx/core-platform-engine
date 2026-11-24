package com.coreplatform.orders.provider;

import com.coreplatform.authorization.config.AuthorizationProperties;
import com.coreplatform.authorization.model.Policy;
import com.coreplatform.authorization.model.Role;
import com.coreplatform.authorization.model.ServiceAccountData;
import com.coreplatform.authorization.model.UserAuthorizationData;
import com.coreplatform.authorization.provider.FallbackDataLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class InternalApiFallbackLoader implements FallbackDataLoader {

    private final RestClient restClient;
    private final ConcurrentHashMap<String, CompletableFuture<?>> inFlight = new ConcurrentHashMap<>();
    private final AtomicBoolean coldStartDone = new AtomicBoolean(false);

    public InternalApiFallbackLoader(AuthorizationProperties properties, String serviceToken) {
        this.restClient = RestClient.builder()
                .baseUrl(properties.getInternalApi().getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + serviceToken)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public InternalApiFallbackLoader(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<UserAuthorizationData> loadUserData(String keycloakSub) {
        applyColdStartJitter();
        String key = "user:" + keycloakSub;
        CompletableFuture<Optional<UserAuthorizationData>> future =
                (CompletableFuture<Optional<UserAuthorizationData>>) inFlight.computeIfAbsent(key, k -> {
                    CompletableFuture<Optional<UserAuthorizationData>> f = CompletableFuture.supplyAsync(() -> {
                        try {
                            UserAuthorizationData data = restClient.get()
                                    .uri("/api/internal/authz-data/user/{sub}", keycloakSub)
                                    .retrieve()
                                    .body(UserAuthorizationData.class);
                            return Optional.ofNullable(data);
                        } catch (Exception e) {
                            log.warn("Failed to load user data from internal API for sub={}", keycloakSub, e);
                            return Optional.empty();
                        }
                    });
                    f.whenComplete((r, ex) -> inFlight.remove(key));
                    return f;
                });
        return future.join();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<ServiceAccountData> loadServiceAccountData(String keycloakSub) {
        applyColdStartJitter();
        String key = "svc:" + keycloakSub;
        CompletableFuture<Optional<ServiceAccountData>> future =
                (CompletableFuture<Optional<ServiceAccountData>>) inFlight.computeIfAbsent(key, k -> {
                    CompletableFuture<Optional<ServiceAccountData>> f = CompletableFuture.supplyAsync(() -> {
                        try {
                            ServiceAccountData data = restClient.get()
                                    .uri("/api/internal/authz-data/service-account/{sub}", keycloakSub)
                                    .retrieve()
                                    .body(ServiceAccountData.class);
                            return Optional.ofNullable(data);
                        } catch (Exception e) {
                            log.warn("Failed to load service account data from internal API for sub={}", keycloakSub, e);
                            return Optional.empty();
                        }
                    });
                    f.whenComplete((r, ex) -> inFlight.remove(key));
                    return f;
                });
        return future.join();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<Role> loadRole(UUID roleId) {
        applyColdStartJitter();
        String key = "role:" + roleId;
        CompletableFuture<Optional<Role>> future =
                (CompletableFuture<Optional<Role>>) inFlight.computeIfAbsent(key, k -> {
                    CompletableFuture<Optional<Role>> f = CompletableFuture.supplyAsync(() -> {
                        try {
                            Role data = restClient.get()
                                    .uri("/api/internal/authz-data/role/{id}", roleId)
                                    .retrieve()
                                    .body(Role.class);
                            return Optional.ofNullable(data);
                        } catch (Exception e) {
                            log.warn("Failed to load role from internal API for id={}", roleId, e);
                            return Optional.empty();
                        }
                    });
                    f.whenComplete((r, ex) -> inFlight.remove(key));
                    return f;
                });
        return future.join();
    }

    @Override
    public List<Policy> loadPolicies(UUID tenantId, String resource, String action) {
        applyColdStartJitter();
        try {
            List<Policy> policies = restClient.get()
                    .uri("/api/internal/authz-data/policies/{tenantId}/{resource}/{action}",
                            tenantId, resource, action)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return policies != null ? policies : List.of();
        } catch (Exception e) {
            log.warn("Failed to load policies from internal API for tenant={} resource={} action={}",
                    tenantId, resource, action, e);
            return List.of();
        }
    }

    @Override
    public Optional<Long> loadTenantAuthzVersion(UUID tenantId) {
        return Optional.empty();
    }

    private void applyColdStartJitter() {
        if (coldStartDone.compareAndSet(false, true)) {
            long jitterMs = ThreadLocalRandom.current().nextLong(0, 500);
            log.info("Cold-start jitter: sleeping {}ms before first internal API call", jitterMs);
            try {
                Thread.sleep(jitterMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
