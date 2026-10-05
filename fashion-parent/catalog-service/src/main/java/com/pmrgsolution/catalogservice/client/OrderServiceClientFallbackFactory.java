package com.pmrgsolution.catalogservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class OrderServiceClientFallbackFactory implements FallbackFactory<OrderServiceClient> {

    @Override
    public OrderServiceClient create(Throwable cause) {
        return new OrderServiceClient() {
            @Override
            public boolean hasPurchasedProduct(String serviceKey, UUID userId, UUID productId) {
                log.warn("OrderServiceClient fallback triggered for userId={}, productId={}. Reason: {}",
                        userId, productId, cause != null ? cause.getMessage() : "Unknown");
                return false;
            }
        };
    }
}
