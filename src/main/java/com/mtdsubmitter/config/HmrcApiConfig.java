package com.mtdsubmitter.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for the HMRC API WebClient.
 */
@Configuration
public class HmrcApiConfig {

    @Value("${hmrc.api.base-url}")
    private String hmrcBaseUrl;

    @Bean
    public WebClient hmrcWebClient() {
        return WebClient.builder()
                .baseUrl(hmrcBaseUrl)
                .defaultHeader("Accept", "application/vnd.hmrc.2.0+json")
                .defaultHeader("Content-Type", "application/json")
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(1024 * 1024)) // 1MB buffer
                .build();
    }
}
