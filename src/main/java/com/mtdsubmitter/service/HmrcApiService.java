package com.mtdsubmitter.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Service to execute Making Tax Digital (MTD) API calls to HMRC.
 * Handles automatic token refreshing, error mapping, and fraud prevention auditing headers.
 */
@Service
public class HmrcApiService {

    private final WebClient webClient;
    private final HmrcAuthService hmrcAuthService;
    private final FraudPreventionService fraudPreventionService;

    public HmrcApiService(WebClient hmrcWebClient,
                          HmrcAuthService hmrcAuthService,
                          FraudPreventionService fraudPreventionService) {
        this.webClient = hmrcWebClient;
        this.hmrcAuthService = hmrcAuthService;
        this.fraudPreventionService = fraudPreventionService;
    }

    /**
     * Retrieve the list of businesses and properties registered for this taxpayer under HMRC.
     * Endpoint: GET /registration/business-details/nino/{nino}
     */
    public Mono<JsonNode> getBusinessDetails(UUID userId, String nino) {
        String cleanedNino = cleanNino(nino);
        String path = "/registration/business-details/nino/" + cleanedNino;
        return executeGet(userId, path);
    }

    /**
     * Retrieve all outstanding obligations (quarterly and annual deadlines) from HMRC.
     * Endpoint: GET /enterprises/obligations/standard/nino/{nino}?status=O
     */
    public Mono<JsonNode> getObligations(UUID userId, String nino, LocalDate fromDate, LocalDate toDate) {
        String cleanedNino = cleanNino(nino);
        String path = String.format("/enterprises/obligations/standard/nino/%s?from=%s&to=%s&status=O",
                cleanedNino, fromDate.toString(), toDate.toString());
        return executeGet(userId, path);
    }

    /**
     * Submit a quarterly update for a Self-Employment business to HMRC.
     * Endpoint: POST /income-tax/nino/{nino}/self-employments/{incomeSourceId}/periodic/{taxYear}
     */
    public Mono<JsonNode> submitSelfEmploymentQuarter(UUID userId, String nino, String incomeSourceId,
                                                      String taxYear, JsonNode payload) {
        String cleanedNino = cleanNino(nino);
        String path = String.format("/income-tax/nino/%s/self-employments/%s/periodic/%s",
                cleanedNino, incomeSourceId, taxYear);
        return executePost(userId, path, payload);
    }

    /**
     * Submit a quarterly update for a UK Property business to HMRC.
     * Endpoint: POST /income-tax/nino/{nino}/properties/{incomeSourceId}/periodic/{taxYear}
     */
    public Mono<JsonNode> submitPropertyQuarter(UUID userId, String nino, String incomeSourceId,
                                                String taxYear, JsonNode payload) {
        String cleanedNino = cleanNino(nino);
        String path = String.format("/income-tax/nino/%s/properties/%s/periodic/%s",
                cleanedNino, incomeSourceId, taxYear);
        return executePost(userId, path, payload);
    }

    /**
     * Trigger a tax liability calculation for a tax year.
     * Endpoint: POST /income-tax/nino/{nino}/calculations/{taxYear}
     */
    public Mono<JsonNode> triggerTaxCalculation(UUID userId, String nino, String taxYear) {
        String cleanedNino = cleanNino(nino);
        String path = String.format("/income-tax/nino/%s/calculations/%s", cleanedNino, taxYear);
        return executePost(userId, path, null);
    }

    /**
     * Retrieve the tax liability calculation details.
     * Endpoint: GET /income-tax/nino/{nino}/calculations/{taxYear}/{calculationId}
     */
    public Mono<JsonNode> getTaxCalculation(UUID userId, String nino, String taxYear, String calculationId) {
        String cleanedNino = cleanNino(nino);
        String path = String.format("/income-tax/nino/%s/calculations/%s/%s",
                cleanedNino, taxYear, calculationId);
        return executeGet(userId, path);
    }

    /**
     * Helper to perform authenticated GET requests with fraud prevention headers.
     */
    private Mono<JsonNode> executeGet(UUID userId, String path) {
        return Mono.defer(() -> {
            String accessToken = hmrcAuthService.getValidAccessToken(userId);
            Map<String, String> fraudHeaders = fraudPreventionService.buildHeaders();

            return webClient.get()
                    .uri(path)
                    .headers(httpHeaders -> {
                        httpHeaders.set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
                        fraudHeaders.forEach(httpHeaders::set);
                    })
                    .retrieve()
                    .bodyToMono(JsonNode.class);
        });
    }

    /**
     * Helper to perform authenticated POST requests with fraud prevention headers.
     */
    private Mono<JsonNode> executePost(UUID userId, String path, Object payload) {
        return Mono.defer(() -> {
            String accessToken = hmrcAuthService.getValidAccessToken(userId);
            Map<String, String> fraudHeaders = fraudPreventionService.buildHeaders();

            WebClient.RequestBodySpec requestSpec = webClient.post()
                    .uri(path)
                    .headers(httpHeaders -> {
                        httpHeaders.set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
                        fraudHeaders.forEach(httpHeaders::set);
                    });

            if (payload != null) {
                return requestSpec.bodyValue(payload)
                        .retrieve()
                        .bodyToMono(JsonNode.class);
            } else {
                return requestSpec.retrieve()
                        .bodyToMono(JsonNode.class);
            }
        });
    }

    /**
     * Helper to clean NINO format (must be uppercase, alphanumeric, no spaces).
     */
    private String cleanNino(String nino) {
        if (nino == null) return "";
        return nino.replaceAll("\\s+", "").toUpperCase();
    }
}
