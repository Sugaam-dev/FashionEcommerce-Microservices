package com.pmrgsolution.catalogservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(
        name = "order-service",
        url = "${feign.order-service.url:}",
        fallbackFactory = OrderServiceClientFallbackFactory.class
)
public interface OrderServiceClient {

    @GetMapping("/api/v1/internal/orders/verify-purchase")
    boolean hasPurchasedProduct(
            @RequestHeader("X-Internal-Service-Key") String serviceKey,
            @RequestParam("userId") UUID userId,
            @RequestParam("productId") UUID productId
    );
}
