package com.ecommerce.api_gateway.error;

import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.ErrorResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
@Order(-2)
public class GatewayErrorWebExceptionHandler
        implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(
            ServerWebExchange exchange,
            Throwable ex
    ) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatusCode statusCode = resolveStatus(ex);
        int status = statusCode.value();

        exchange.getResponse().setStatusCode(statusCode);
        exchange.getResponse()
                .getHeaders()
                .setContentType(MediaType.APPLICATION_JSON);

        String body = """
                {
                  "status": %d,
                  "error": "%s",
                  "message": "%s",
                  "path": "%s",
                  "timestamp": "%s"
                }
                """.formatted(
                status,
                escape(reasonPhrase(status)),
                escape(messageFor(status)),
                escape(exchange.getRequest().getURI().getPath()),
                Instant.now()
        );

        DataBuffer buffer = exchange.getResponse()
                .bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));

        return exchange.getResponse()
                .writeWith(Mono.just(buffer));
    }

    private HttpStatusCode resolveStatus(Throwable ex) {
        if (hasCause(ex, ConnectException.class)) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }

        Throwable current = ex;

        while (current != null) {
            if (current instanceof ErrorResponse errorResponse) {
                return errorResponse.getStatusCode();
            }

            current = current.getCause();
        }

        return HttpStatus.BAD_GATEWAY;
    }

    private String messageFor(int status) {
        return switch (status) {
            case 404 -> "No gateway route found for request";
            case 503 -> "Downstream service is unavailable";
            case 504 -> "Downstream service response timed out";
            case 502 -> "Gateway failed to process the request";
            default -> "Request could not be processed";
        };
    }

    private String reasonPhrase(int status) {
        HttpStatus resolved = HttpStatus.resolve(status);

        return resolved != null
                ? resolved.getReasonPhrase()
                : "HTTP " + status;
    }

    private boolean hasCause(
            Throwable throwable,
            Class<? extends Throwable> type
    ) {
        Throwable current = throwable;

        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }

    private String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
