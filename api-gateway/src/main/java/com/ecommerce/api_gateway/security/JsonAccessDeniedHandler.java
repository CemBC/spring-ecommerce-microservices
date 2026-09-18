package com.ecommerce.api_gateway.security;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
public class JsonAccessDeniedHandler
        implements ServerAccessDeniedHandler {

    @Override
    public Mono<Void> handle(
            ServerWebExchange exchange,
            AccessDeniedException denied
    ) {
        exchange.getResponse()
                .setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse()
                .getHeaders()
                .setContentType(MediaType.APPLICATION_JSON);

        String body = """
                {
                  "status": 403,
                  "error": "Forbidden",
                  "message": "You do not have permission to access this resource",
                  "path": "%s",
                  "timestamp": "%s"
                }
                """.formatted(
                escape(exchange.getRequest().getURI().getPath()),
                Instant.now()
        );

        DataBuffer buffer = exchange.getResponse()
                .bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));

        return exchange.getResponse()
                .writeWith(Mono.just(buffer));
    }

    private String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
