package com.coreplatform.orders.config;

import com.coreplatform.authorization.config.AuthorizationProperties;
import com.coreplatform.authorization.provider.FallbackDataLoader;
import com.coreplatform.orders.provider.InternalApiFallbackLoader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AuthorizationConfig {

    @Bean
    @ConditionalOnProperty(prefix = "core-platform.authorization.internal-api", name = "base-url")
    public FallbackDataLoader fallbackDataLoader(AuthorizationProperties properties) {
        RestClient restClient = RestClient.builder()
                .baseUrl(properties.getInternalApi().getBaseUrl())
                .build();
        return new InternalApiFallbackLoader(restClient);
    }
}
