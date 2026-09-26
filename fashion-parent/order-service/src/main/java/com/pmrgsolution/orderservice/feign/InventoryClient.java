package com.pmrgsolution.orderservice.feign;

import com.pmrgsolution.orderservice.dto.request.ConfirmStockRequest;
import com.pmrgsolution.orderservice.dto.request.ReleaseStockRequest;
import com.pmrgsolution.orderservice.dto.request.ReserveStockRequest;
import com.pmrgsolution.orderservice.dto.response.ReservationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "inventory-service", url = "${feign.inventory-service.url:http://localhost:8085}")
public interface InventoryClient {

    @PostMapping("/api/v1/internal/inventory/reserve")
    ReservationResponse reserveStock(
            @RequestHeader("X-Internal-Service-Key") String key,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody ReserveStockRequest req
    );

    @PostMapping("/api/v1/internal/inventory/confirm")
    void confirmReservation(
            @RequestHeader("X-Internal-Service-Key") String key,
            @RequestBody ConfirmStockRequest req
    );

    @PostMapping("/api/v1/internal/inventory/release")
    void releaseReservation(
            @RequestHeader("X-Internal-Service-Key") String key,
            @RequestBody ReleaseStockRequest req
    );
}
