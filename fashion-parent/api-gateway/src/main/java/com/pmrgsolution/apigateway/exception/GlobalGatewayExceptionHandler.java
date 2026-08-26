package com.pmrgsolution.apigateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Enterprise Gateway Error Handler.
 * Intercepts routing failures, service timeouts, and 404s, returning uniform JSON envelopes
 * instead of raw Netty HTML stack traces.
 */
@Component
@Order(-2) // Runs before default Spring Boot error handler
public class GlobalGatewayExceptionHandler implements WebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalGatewayExceptionHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String errorCode = "GATEWAY_ERROR";
        String message = "An error occurred in the API Gateway.";

        if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.resolve(rse.getStatusCode().value());
            if (status == null) {
                status = HttpStatus.INTERNAL_SERVER_ERROR;
            }
            if (status == HttpStatus.NOT_FOUND) {
                errorCode = "ROUTE_NOT_FOUND";
                message = "The requested endpoint was not found on the platform.";
            } else if (status.is5xxServerError()) {
                errorCode = "SERVICE_UNAVAILABLE";
                message = "The requested microservice is currently unavailable. Please try again later.";
            }
        } else if (ex.getClass().getSimpleName().contains("NotFoundException") ||
                   ex.getMessage() != null && ex.getMessage().contains("Unable to find instance")) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            errorCode = "SERVICE_UNAVAILABLE";
            message = "The requested microservice is currently offline or unreachable.";
        } else if (ex.getClass().getSimpleName().contains("TimeoutException")) {
            status = HttpStatus.GATEWAY_TIMEOUT;
            errorCode = "GATEWAY_TIMEOUT";
            message = "The microservice timed out while processing your request.";
        }

        log.error("Gateway error [{} - {}] on path {}: {}", errorCode, status.value(),
                exchange.getRequest().getURI().getPath(), ex.getMessage());

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("errorCode", errorCode);
        errorBody.put("message", message);
        errorBody.put("timestamp", Instant.now().toString());

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", false);
        responseBody.put("status", status.value());
        responseBody.put("data", null);
        responseBody.put("error", errorBody);
        responseBody.put("path", exchange.getRequest().getURI().getPath());
        responseBody.put("timestamp", Instant.now().toString());

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(responseBody);
        } catch (JsonProcessingException e) {
            bytes = ("{\"success\":false,\"status\":500,\"message\":\"Serialization Error\"}").getBytes();
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
