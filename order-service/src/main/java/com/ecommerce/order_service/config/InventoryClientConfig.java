package com.ecommerce.order_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class InventoryClientConfig {

    @Bean
    public RestClient inventoryRestClient(
            RestClient.Builder builder,
            @Value("${services.inventory.url}") String inventoryServiceUrl,
            @Value("${services.inventory.connect-timeout-ms:2000}") long connectTimeoutMs,
            @Value("${services.inventory.read-timeout-ms:3000}") long readTimeoutMs
    ) {
        HttpClient httpClient =
                HttpClient.newBuilder()
                        .connectTimeout(
                                Duration.ofMillis(
                                        connectTimeoutMs
                                )
                        )
                        .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(
                        httpClient
                );

        requestFactory.setReadTimeout(
                Duration.ofMillis(
                        readTimeoutMs
                )
        );

        return builder
                .baseUrl(inventoryServiceUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
