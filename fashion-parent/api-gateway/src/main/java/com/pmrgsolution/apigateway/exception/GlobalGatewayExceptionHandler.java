package com.pmrgsolution.apigateway.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@Order(-2)
@RequiredArgsConstructor
public class GlobalGatewayExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String errorCode = "INTERNAL_SERVER_ERROR";
        String userFriendlyMessage = "An internal gateway error occurred";

        if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.valueOf(rse.getStatusCode().value());
            if (status == HttpStatus.NOT_FOUND) {
                errorCode = "ROUTE_NOT_FOUND";
                userFriendlyMessage = "The requested endpoint or route was not found";
            } else if (status == HttpStatus.TOO_MANY_REQUESTS) {
                errorCode = "RATE_LIMIT_EXCEEDED";
                userFriendlyMessage = "Too many requests. Please slow down and try again shortly.";
            } else if (status == HttpStatus.SERVICE_UNAVAILABLE) {
                errorCode = "SERVICE_UNAVAILABLE";
                userFriendlyMessage = "Target service is currently unavailable. Please retry shortly.";
            } else {
                errorCode = status.name();
                userFriendlyMessage = rse.getReason() != null ? rse.getReason() : status.getReasonPhrase();
            }
        } else if (ex instanceof java.net.ConnectException || ex instanceof java.util.concurrent.TimeoutException) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            errorCode = "SERVICE_UNAVAILABLE";
            userFriendlyMessage = "Target microservice connection failed or timed out";
        }

        log.error("Gateway error [Status: {}, ErrorCode: {}] on path {}: {}",
                status.value(), errorCode, exchange.getRequest().getPath(), ex.getMessage());

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("errorCode", errorCode);
        errorDetails.put("message", userFriendlyMessage);
        errorDetails.put("timestamp", Instant.now().toString());

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", false);
        responseBody.put("status", status.value());
        responseBody.put("message", userFriendlyMessage);
        responseBody.put("data", null);
        responseBody.put("error", errorDetails);
        responseBody.put("path", exchange.getRequest().getPath().value());

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(responseBody);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            log.error("Failed to serialize gateway error response: {}", e.getMessage(), e);
            byte[] fallback = "{\"success\":false,\"status\":500,\"message\":\"Internal Server Error\"}".getBytes(StandardCharsets.UTF_8);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(fallback)));
        }
    }
}
