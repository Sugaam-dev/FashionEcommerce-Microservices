package com.pmrgsolution.orderservice.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrgsolution.shared.core.response.ApiResponse;
import feign.Logger;
import feign.Request;
import feign.RequestInterceptor;
import feign.Response;
import feign.Retryer;
import feign.Util;
import feign.codec.Decoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.ParameterizedType;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableFeignClients(basePackages = "com.pmrgsolution.orderservice")
public class FeignConfig {

    @Value("${app.security.internal-service-key:fashion-internal-secret-key-2026-v1}")
    private String internalServiceKey;

    @Bean
    public Request.Options feignOptions() {
        return new Request.Options(
                2, TimeUnit.SECONDS,  // Connect timeout: 2s
                5, TimeUnit.SECONDS,  // Read timeout: 5s
                true                  // Follow redirects
        );
    }

    @Bean
    public Retryer feignRetryer() {
        return new Retryer.Default(100, 1000, 3);
    }

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    @Bean
    public RequestInterceptor internalServiceKeyInterceptor() {
        return template -> {
            if (!template.headers().containsKey("X-Internal-Service-Key")) {
                template.header("X-Internal-Service-Key", internalServiceKey);
            }
        };
    }

    @Bean
    public Decoder feignDecoder(ObjectMapper objectMapper) {
        Decoder defaultDecoder = new SpringDecoder(HttpMessageConverters::new);
        return (response, type) -> {
            if (response.body() == null) {
                return null;
            }

            // If the target type is already ApiResponse, defer to default decoder
            if (type instanceof Class<?> clazz && ApiResponse.class.isAssignableFrom(clazz)) {
                return defaultDecoder.decode(response, type);
            }
            if (type instanceof ParameterizedType pt && pt.getRawType() instanceof Class<?> clazz && ApiResponse.class.isAssignableFrom(clazz)) {
                return defaultDecoder.decode(response, type);
            }

            byte[] bodyBytes = Util.toByteArray(response.body().asInputStream());
            if (bodyBytes.length == 0) {
                return null;
            }

            try {
                JsonNode rootNode = objectMapper.readTree(bodyBytes);
                if (rootNode.isObject() && rootNode.has("data") && (rootNode.has("success") || rootNode.has("status"))) {
                    JsonNode dataNode = rootNode.get("data");
                    if (dataNode.isNull() || dataNode.isMissingNode()) {
                        return null;
                    }
                    return objectMapper.readerFor(objectMapper.constructType(type)).readValue(dataNode);
                }
            } catch (Exception ignored) {
                // fall back to default decoder
            }

            Response copy = response.toBuilder().body(bodyBytes).build();
            return defaultDecoder.decode(copy, type);
        };
    }
}
