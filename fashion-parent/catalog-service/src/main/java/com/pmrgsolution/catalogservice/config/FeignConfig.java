package com.pmrgsolution.catalogservice.config;

import feign.Request;
import feign.RequestInterceptor;
import feign.Retryer;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Configuration
public class FeignConfig {

    @Value("${app.internal.service-key:fashion-internal-microservice-secret-key-2026}")
    private String internalServiceKey;

    @Bean
    public Request.Options feignOptions() {
        return new Request.Options(
                2, TimeUnit.SECONDS,
                5, TimeUnit.SECONDS,
                true
        );
    }

    @Bean
    public Retryer feignRetryer() {
        return new Retryer.Default(100, 1000, 3);
    }

    @Bean
    public RequestInterceptor feignRequestInterceptor() {
        return requestTemplate -> {
            requestTemplate.header("X-Internal-Service-Key", internalServiceKey);
            String correlationId = MDC.get("correlationId");
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }
            requestTemplate.header("X-Correlation-Id", correlationId);
        };
    }
}
