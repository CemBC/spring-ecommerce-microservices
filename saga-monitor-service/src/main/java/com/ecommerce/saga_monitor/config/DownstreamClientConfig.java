package com.ecommerce.saga_monitor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class DownstreamClientConfig {

    @Bean
    public RestClient sagaOrderRestClient(
            RestClient.Builder builder,
            @Value("${services.order.url}")
            String url
    ) {
        return builder
                .baseUrl(url)
                .build();
    }

    @Bean
    public RestClient sagaInventoryRestClient(
            RestClient.Builder builder,
            @Value("${services.inventory.url}")
            String url
    ) {
        return builder
                .baseUrl(url)
                .build();
    }

    @Bean
    public RestClient sagaPaymentRestClient(
            RestClient.Builder builder,
            @Value("${services.payment.url}")
            String url
    ) {
        return builder
                .baseUrl(url)
                .build();
    }
}
