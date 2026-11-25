package com.coreplatform.authorization.config;

import com.coreplatform.authorization.aspect.AuthorizeAspect;
import com.coreplatform.authorization.audit.AuditLogger;
import com.coreplatform.authorization.cache.AuthorizationRedisService;
import com.coreplatform.authorization.cache.CacheInvalidationListener;
import com.coreplatform.authorization.cache.CaffeineAuthorizationCache;
import com.coreplatform.authorization.cache.RedisKeyBuilder;
import com.coreplatform.authorization.engine.AuthorizationEngine;
import com.coreplatform.authorization.event.RedisChangePublisher;
import com.coreplatform.authorization.guard.TenantGuard;
import com.coreplatform.authorization.policy.ConditionEvaluator;
import com.coreplatform.authorization.policy.PolicyCompiler;
import com.coreplatform.authorization.policy.PolicyEvaluator;
import com.coreplatform.authorization.provider.AuthorizationDataProvider;
import com.coreplatform.authorization.provider.FallbackDataLoader;
import com.coreplatform.authorization.provider.RedisAuthorizationDataProvider;
import com.coreplatform.authorization.rbac.RbacEvaluator;
import com.coreplatform.authorization.resolver.AllScopeEvaluator;
import com.coreplatform.authorization.resolver.DataScopeResolver;
import com.coreplatform.authorization.resolver.DepartmentScopeEvaluator;
import com.coreplatform.authorization.resolver.OrgScopeEvaluator;
import com.coreplatform.authorization.resolver.OwnScopeEvaluator;
import com.coreplatform.authorization.resolver.ResourceAttributeResolver;
import com.coreplatform.authorization.resolver.ScopeEnforcer;
import com.coreplatform.authorization.resolver.ScopeEvaluator;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.Collections;
import java.util.List;

@AutoConfiguration(after = {
        org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
        org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration.class
})
@EnableConfigurationProperties(AuthorizationProperties.class)
@EnableAspectJAutoProxy
public class AuthorizationAutoConfiguration {

    @Bean
    public CaffeineAuthorizationCache caffeineAuthorizationCache(AuthorizationProperties properties) {
        return new CaffeineAuthorizationCache(
                properties.getCaffeine().getTtl(),
                properties.getCaffeine().getMaxSize()
        );
    }

    @Bean
    @ConditionalOnBean(RedisConnectionFactory.class)
    public StringRedisTemplate authorizationRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    @ConditionalOnBean(RedisConnectionFactory.class)
    public AuthorizationRedisService authorizationRedisService(StringRedisTemplate authorizationRedisTemplate,
                                                               ObjectMapper objectMapper,
                                                               MeterRegistry meterRegistry,
                                                               AuthorizationProperties properties) {
        return new AuthorizationRedisService(authorizationRedisTemplate, objectMapper, meterRegistry,
                properties.getRedis().getCacheTtl().toSeconds());
    }

    @Bean
    @ConditionalOnBean({RedisConnectionFactory.class, FallbackDataLoader.class})
    public RedisAuthorizationDataProvider redisAuthorizationDataProvider(
            AuthorizationRedisService authorizationRedisService,
            CaffeineAuthorizationCache caffeineAuthorizationCache,
            FallbackDataLoader fallbackDataLoader,
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            MeterRegistry meterRegistry) {
        return new RedisAuthorizationDataProvider(
                authorizationRedisService,
                caffeineAuthorizationCache,
                fallbackDataLoader,
                objectMapper,
                circuitBreakerRegistry,
                meterRegistry
        );
    }

    @Bean
    @ConditionalOnBean(RedisConnectionFactory.class)
    public CacheInvalidationListener cacheInvalidationListener(CaffeineAuthorizationCache caffeineAuthorizationCache,
                                                                ObjectMapper objectMapper) {
        return new CacheInvalidationListener(caffeineAuthorizationCache, objectMapper);
    }

    @Bean
    @ConditionalOnBean(RedisConnectionFactory.class)
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            CacheInvalidationListener cacheInvalidationListener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(cacheInvalidationListener,
                new ChannelTopic(RedisKeyBuilder.invalidationChannel()));
        return container;
    }

    @Bean
    @ConditionalOnBean(RedisConnectionFactory.class)
    public RedisChangePublisher redisChangePublisher(AuthorizationRedisService authorizationRedisService,
                                                     ObjectMapper objectMapper) {
        return new RedisChangePublisher(authorizationRedisService, objectMapper);
    }

    @Bean
    public TenantGuard tenantGuard() {
        return new TenantGuard();
    }

    @Bean
    @ConditionalOnMissingBean
    public AllScopeEvaluator allScopeEvaluator() {
        return new AllScopeEvaluator();
    }

    @Bean
    @ConditionalOnMissingBean
    public OwnScopeEvaluator ownScopeEvaluator() {
        return new OwnScopeEvaluator();
    }

    @Bean
    @ConditionalOnMissingBean
    public DepartmentScopeEvaluator departmentScopeEvaluator() {
        return new DepartmentScopeEvaluator();
    }

    @Bean
    @ConditionalOnMissingBean
    public OrgScopeEvaluator orgScopeEvaluator() {
        return new OrgScopeEvaluator();
    }

    @Bean
    public ScopeEnforcer scopeEnforcer(List<ScopeEvaluator> evaluators) {
        return new ScopeEnforcer(evaluators);
    }

    @Bean
    public DataScopeResolver dataScopeResolver(List<ScopeEvaluator> evaluators) {
        return new DataScopeResolver(evaluators);
    }

    @Bean
    @ConditionalOnBean(AuthorizationDataProvider.class)
    public RbacEvaluator rbacEvaluator(AuthorizationDataProvider authorizationDataProvider) {
        return new RbacEvaluator(authorizationDataProvider);
    }

    @Bean
    public ConditionEvaluator conditionEvaluator() {
        return new ConditionEvaluator();
    }

    @Bean
    public PolicyCompiler policyCompiler(ObjectMapper objectMapper) {
        return new PolicyCompiler(objectMapper);
    }

    @Bean
    @ConditionalOnBean(AuthorizationDataProvider.class)
    public PolicyEvaluator policyEvaluator(AuthorizationDataProvider authorizationDataProvider,
                                            PolicyCompiler policyCompiler,
                                            ConditionEvaluator conditionEvaluator,
                                            ObjectMapper objectMapper,
                                            MeterRegistry meterRegistry) {
        return new PolicyEvaluator(authorizationDataProvider, policyCompiler, conditionEvaluator, objectMapper, meterRegistry);
    }

    @Bean
    @ConditionalOnBean({AuthorizationDataProvider.class, AuditLogger.class})
    public AuthorizationEngine authorizationEngine(AuthorizationDataProvider authorizationDataProvider,
                                                    TenantGuard tenantGuard,
                                                    RbacEvaluator rbacEvaluator,
                                                    ScopeEnforcer scopeEnforcer,
                                                    PolicyEvaluator policyEvaluator,
                                                    AuditLogger auditLogger,
                                                    MeterRegistry meterRegistry) {
        return new AuthorizationEngine(authorizationDataProvider, tenantGuard, rbacEvaluator, scopeEnforcer, policyEvaluator, auditLogger, meterRegistry);
    }

    @Bean
    @ConditionalOnBean(AuthorizationEngine.class)
    public AuthorizeAspect authorizeAspect(AuthorizationEngine authorizationEngine,
                                           List<ResourceAttributeResolver> resolvers) {
        return new AuthorizeAspect(authorizationEngine, resolvers != null ? resolvers : Collections.emptyList());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Bean
    @ConditionalOnBean(MeterRegistry.class)
    public CaffeineCacheMetrics caffeineCacheMetrics(CaffeineAuthorizationCache caffeineAuthorizationCache,
                                                     MeterRegistry meterRegistry) {
        CaffeineCacheMetrics metrics = new CaffeineCacheMetrics(caffeineAuthorizationCache.unwrap(), "authz.caffeine", Collections.emptyList());
        metrics.bindTo(meterRegistry);
        return metrics;
    }
}
