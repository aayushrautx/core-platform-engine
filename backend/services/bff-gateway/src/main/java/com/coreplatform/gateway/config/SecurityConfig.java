package com.coreplatform.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.oidc.web.server.logout.OidcClientInitiatedServerLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.DefaultServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.security.web.server.csrf.ServerCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.server.DelegatingServerAuthenticationEntryPoint;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Bean
    public ServerOAuth2AuthorizationRequestResolver pkceAuthorizationRequestResolver(
            ReactiveClientRegistrationRepository clientRegistrationRepository) {
        DefaultServerOAuth2AuthorizationRequestResolver resolver =
                new DefaultServerOAuth2AuthorizationRequestResolver(clientRegistrationRepository);
        resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
        return resolver;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                         ReactiveClientRegistrationRepository clientRegistrationRepository,
                                                         ServerOAuth2AuthorizationRequestResolver pkceAuthorizationRequestResolver) {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieServerCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new ServerCsrfTokenRequestAttributeHandler())
                )
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyExchange().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint())
                )
                .oauth2Login(oauth2 -> oauth2
                        .authorizationRequestResolver(pkceAuthorizationRequestResolver)
                        .authenticationSuccessHandler(
                                new RedirectServerAuthenticationSuccessHandler(frontendUrl))
                )
                .logout(logout -> logout
                        .logoutSuccessHandler(oidcLogoutSuccessHandler(clientRegistrationRepository))
                );

        return http.build();
    }

    private ServerLogoutSuccessHandler oidcLogoutSuccessHandler(
            ReactiveClientRegistrationRepository clientRegistrationRepository) {
        OidcClientInitiatedServerLogoutSuccessHandler handler =
                new OidcClientInitiatedServerLogoutSuccessHandler(clientRegistrationRepository);
        handler.setPostLogoutRedirectUri(frontendUrl + "/login");
        return handler;
    }

    private ServerAuthenticationEntryPoint authenticationEntryPoint() {
        DelegatingServerAuthenticationEntryPoint.DelegateEntry apiEntry =
                new DelegatingServerAuthenticationEntryPoint.DelegateEntry(
                        ServerWebExchangeMatchers.pathMatchers("/api/**"),
                        new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED)
                );
        DelegatingServerAuthenticationEntryPoint entryPoint =
                new DelegatingServerAuthenticationEntryPoint(apiEntry);
        entryPoint.setDefaultEntryPoint(
                new org.springframework.security.web.server.authentication.RedirectServerAuthenticationEntryPoint(
                        "/oauth2/authorization/keycloak"));
        return entryPoint;
    }

    @Bean
    public WebFilter csrfCookieWebFilter() {
        return (exchange, chain) -> {
            Mono<CsrfToken> csrfToken = exchange.getAttribute(CsrfToken.class.getName());
            if (csrfToken != null) {
                return csrfToken.then(chain.filter(exchange));
            }
            return chain.filter(exchange);
        };
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(frontendUrl));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
