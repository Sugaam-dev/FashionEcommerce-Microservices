package com.pmrgsolution.apigateway.controller;

import com.pmrgsolution.shared.core.exception.ErrorCode;
import com.pmrgsolution.shared.core.response.ApiResponse;
import com.pmrgsolution.shared.core.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    private ResponseEntity<ApiResponse<Void>> buildFallbackResponse(String serviceName) {
        String msg = serviceName + " is temporarily unavailable. Please try again later.";
        ErrorResponse error = new ErrorResponse(ErrorCode.SERVICE_UNAVAILABLE, msg, null, Instant.now());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.failure(503, msg, error));
    }

    @RequestMapping(value = "/auth", method = {RequestMethod.GET, RequestMethod.POST})
    public Mono<ResponseEntity<ApiResponse<Void>>> authFallback() {
        return Mono.just(buildFallbackResponse("Authentication service"));
    }

    @GetMapping("/catalog")
    public Mono<ResponseEntity<ApiResponse<Void>>> catalogFallback() {
        return Mono.just(buildFallbackResponse("Catalog service"));
    }

    @RequestMapping(value = "/order", method = {RequestMethod.GET, RequestMethod.POST})
    public Mono<ResponseEntity<ApiResponse<Void>>> orderFallback() {
        return Mono.just(buildFallbackResponse("Order service"));
    }

    @RequestMapping(value = "/payment", method = {RequestMethod.GET, RequestMethod.POST})
    public Mono<ResponseEntity<ApiResponse<Void>>> paymentFallback() {
        return Mono.just(buildFallbackResponse("Payment service"));
    }

    @RequestMapping(value = "/coupon", method = {RequestMethod.GET, RequestMethod.POST})
    public Mono<ResponseEntity<ApiResponse<Void>>> couponFallback() {
        return Mono.just(buildFallbackResponse("Coupon service"));
    }

    @RequestMapping(value = "/inventory", method = {RequestMethod.GET, RequestMethod.POST})
    public Mono<ResponseEntity<ApiResponse<Void>>> inventoryFallback() {
        return Mono.just(buildFallbackResponse("Inventory service"));
    }

    @RequestMapping(value = "/notification", method = {RequestMethod.GET, RequestMethod.POST})
    public Mono<ResponseEntity<ApiResponse<Void>>> notificationFallback() {
        return Mono.just(buildFallbackResponse("Notification service"));
    }
}
