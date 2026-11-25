package com.coreplatform.users.config;

import com.coreplatform.authorization.config.AuthorizationProperties;
import com.coreplatform.users.filter.UserSyncFilter;
import com.coreplatform.users.service.UserSyncService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                    UserSyncFilter userSyncFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))
                .addFilterAfter(userSyncFilter, BearerTokenAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public UserSyncFilter userSyncFilter(UserSyncService userSyncService,
                                          AuthorizationProperties properties) {
        return new UserSyncFilter(userSyncService, properties);
    }
}
