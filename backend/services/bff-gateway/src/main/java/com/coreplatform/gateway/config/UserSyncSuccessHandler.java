package com.coreplatform.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
public class UserSyncSuccessHandler extends RedirectServerAuthenticationSuccessHandler {

    private final WebClient webClient;
    private final ServerOAuth2AuthorizedClientRepository authorizedClientRepository;

    public UserSyncSuccessHandler(String frontendUrl,
                                  WebClient.Builder webClientBuilder,
                                  String usersServiceUrl,
                                  ServerOAuth2AuthorizedClientRepository authorizedClientRepository) {
        super(frontendUrl);
        this.webClient = webClientBuilder.baseUrl(usersServiceUrl).build();
        this.authorizedClientRepository = authorizedClientRepository;
    }

    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange,
                                               Authentication authentication) {
        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            return super.onAuthenticationSuccess(webFilterExchange, authentication);
        }

        String clientRegistrationId = oauthToken.getAuthorizedClientRegistrationId();

        return authorizedClientRepository
                .loadAuthorizedClient(clientRegistrationId, authentication,
                        webFilterExchange.getExchange())
                .flatMap(authorizedClient -> {
                    String accessToken = authorizedClient.getAccessToken().getTokenValue();
                    return callUserSync(accessToken);
                })
                .onErrorResume(e -> {
                    log.error("User sync failed, proceeding with login", e);
                    return Mono.empty();
                })
                .then(super.onAuthenticationSuccess(webFilterExchange, authentication));
    }

    private Mono<String> callUserSync(String accessToken) {
        return webClient.post()
                .uri("/api/internal/user-sync")
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(body -> log.info("User sync completed: {}", body))
                .doOnError(e -> log.warn("User sync request failed: {}", e.getMessage()));
    }
}
