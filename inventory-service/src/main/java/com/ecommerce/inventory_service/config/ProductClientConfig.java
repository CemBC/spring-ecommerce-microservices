package com.ecommerce.inventory_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.http.client.JdkClientHttpRequestFactory;

@Configuration
public class ProductClientConfig {

    @Bean
    public RestClient productRestClient(
            RestClient.Builder builder,
            @Value("${services.product.url}") String productServiceUrl,
            @Value("${services.product.connect-timeout-ms:2000}") long connectTimeoutMs,
            @Value("${services.product.read-timeout-ms:3000}") long readTimeoutMs
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(
                Duration.ofMillis(readTimeoutMs)
        );

        return builder
                .baseUrl(productServiceUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
