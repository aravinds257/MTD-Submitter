package com.mtdsubmitter.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mtdsubmitter.model.HmrcToken;
import com.mtdsubmitter.model.User;
import com.mtdsubmitter.repository.HmrcTokenRepository;
import com.mtdsubmitter.repository.UserRepository;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service to handle HMRC OAuth 2.0 flows, token exchanges, storage, and token refreshing.
 */
@Service
public class HmrcAuthService {

    private final HmrcTokenRepository hmrcTokenRepository;
    private final UserRepository userRepository;
    private final WebClient webClient;
    private final AuditService auditService;

    @Value("${spring.security.oauth2.client.registration.hmrc.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.hmrc.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.hmrc.redirect-uri}")
    private String redirectUri;

    @Value("${spring.security.oauth2.client.provider.hmrc.authorization-uri}")
    private String authorizationUri;

    @Value("${spring.security.oauth2.client.provider.hmrc.token-uri}")
    private String tokenUri;

    public HmrcAuthService(HmrcTokenRepository hmrcTokenRepository,
                           UserRepository userRepository,
                           WebClient hmrcWebClient,
                           AuditService auditService) {
        this.hmrcTokenRepository = hmrcTokenRepository;
        this.userRepository = userRepository;
        this.webClient = hmrcWebClient;
        this.auditService = auditService;
    }

    /**
     * Build the authorization redirect URL to send users to HMRC Government Gateway.
     */
    public String getAuthorizationUrl(String state) {
        return UriComponentsBuilder.fromHttpUrl(authorizationUri)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", "read:self-assessment write:self-assessment")
                .queryParam("response_type", "code")
                .queryParam("state", state)
                .build()
                .toUriString();
    }

    /**
     * Exchange the authorization code returned by HMRC for access/refresh tokens.
     */
    @Transactional
    public Mono<HmrcToken> exchangeCode(UUID userId, String code) {
        return webClient.post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("grant_type=authorization_code" +
                           "&client_id=" + clientId +
                           "&client_secret=" + clientSecret +
                           "&redirect_uri=" + redirectUri +
                           "&code=" + code)
                .retrieve()
                .bodyToMono(HmrcTokenResponse.class)
                .map(response -> saveOrUpdateToken(userId, response))
                .doOnSuccess(token -> auditService.log(userId, "HmrcToken", token.getId(), "CONNECT_HMRC", null, null));
    }

    /**
     * Get a valid (not expired) access token for a user. If expired, it is refreshed.
     */
    @Transactional
    public String getValidAccessToken(UUID userId) {
        HmrcToken token = hmrcTokenRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("HMRC connection required. Please connect your account first."));

        // If token expires in less than 5 minutes, refresh it
        if (token.getExpiresAt().isBefore(LocalDateTime.now().plusMinutes(5))) {
            return refreshToken(userId, token).getAccessTokenEncrypted();
        }

        return token.getAccessTokenEncrypted();
    }

    @Transactional
    public HmrcToken refreshToken(UUID userId, HmrcToken token) {
        HmrcTokenResponse response = webClient.post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("grant_type=refresh_token" +
                           "&client_id=" + clientId +
                           "&client_secret=" + clientSecret +
                           "&refresh_token=" + token.getRefreshTokenEncrypted())
                .retrieve()
                .bodyToMono(HmrcTokenResponse.class)
                .block(); // Blocking is acceptable inside transactional context for this single call

        if (response == null) {
            throw new RuntimeException("Failed to refresh HMRC OAuth token");
        }

        return saveOrUpdateToken(userId, response);
    }

    private HmrcToken saveOrUpdateToken(UUID userId, HmrcTokenResponse response) {
        User user = userRepository.getReferenceById(userId);

        HmrcToken token = hmrcTokenRepository.findByUserId(userId)
                .orElseGet(() -> HmrcToken.builder().user(user).build());

        token.setAccessTokenEncrypted(response.getAccessToken());
        token.setRefreshTokenEncrypted(response.getRefreshToken());
        token.setTokenType(response.getTokenType());
        token.setExpiresAt(LocalDateTime.now().plusSeconds(response.getExpiresIn()));
        token.setScope(response.getScope());

        return hmrcTokenRepository.save(token);
    }

    @Data
    public static class HmrcTokenResponse {
        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("refresh_token")
        private String refreshToken;

        @JsonProperty("expires_in")
        private long expiresIn;

        @JsonProperty("token_type")
        private String tokenType;

        @JsonProperty("scope")
        private String scope;
    }
}
