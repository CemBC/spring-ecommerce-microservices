package com.ecommerce.saga_monitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SagaMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                SagaMonitorApplication.class,
                args
        );
    }
}
