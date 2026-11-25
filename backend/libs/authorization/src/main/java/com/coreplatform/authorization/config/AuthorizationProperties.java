package com.coreplatform.authorization.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@ConfigurationProperties(prefix = "core-platform.authorization")
public class AuthorizationProperties {

    private TenantMode tenantMode = TenantMode.MULTI_TENANT;

    private SingleTenant singleTenant = new SingleTenant();

    private Bootstrap bootstrap = new Bootstrap();

    private Freshness freshness = new Freshness();

    private CaffeineConfig caffeine = new CaffeineConfig();

    private Fallback fallback = new Fallback();

    private InternalApi internalApi = new InternalApi();

    private Audit audit = new Audit();

    public enum TenantMode {
        SINGLE_TENANT,
        MULTI_TENANT
    }

    public enum ExceededAction {
        DENY,
        QUEUE
    }

    public enum AuditLevel {
        BASIC,
        DETAILED
    }

    @Getter
    @Setter
    public static class SingleTenant {
        private UUID tenantId;
    }

    @Getter
    @Setter
    public static class Bootstrap {
        private String superAdminSub;
    }

    @Getter
    @Setter
    public static class Freshness {
        private Duration read = Duration.ofSeconds(30);
        private Duration write = Duration.ofSeconds(5);
        private Duration admin = Duration.ZERO;
        private Duration critical = Duration.ZERO;
    }

    @Getter
    @Setter
    public static class CaffeineConfig {
        private long maxSize = 10000;
        private Duration ttl = Duration.ofSeconds(30);
    }

    @Getter
    @Setter
    public static class RedisConfig {
        private Duration cacheTtl = Duration.ofHours(1);
    }

    private RedisConfig redis = new RedisConfig();

    @Getter
    @Setter
    public static class Fallback {
        private int maxConcurrent = 20;
        private int maxQps = 100;
        private Duration queueTimeout = Duration.ofMillis(500);
        private ExceededAction exceededAction = ExceededAction.DENY;
    }

    @Getter
    @Setter
    public static class InternalApi {
        private String baseUrl;
        private List<String> allowedClients = List.of();
    }

    @Getter
    @Setter
    public static class Audit {
        private AuditLevel defaultLevel = AuditLevel.BASIC;
    }
}
