package com.ecommerce.order_service.outbox;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(
        name = "outbox.enabled",
        havingValue = "true"
)
public class OutboxConfiguration {
}
