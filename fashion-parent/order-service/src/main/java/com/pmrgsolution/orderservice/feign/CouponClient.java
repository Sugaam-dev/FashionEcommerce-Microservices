package com.pmrgsolution.orderservice.feign;

import com.pmrgsolution.orderservice.dto.request.RedeemCouponRequest;
import com.pmrgsolution.orderservice.dto.request.ValidateCouponRequest;
import com.pmrgsolution.orderservice.dto.response.CouponValidationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "coupon-service", url = "${feign.coupon-service.url:http://localhost:8086}")
public interface CouponClient {

    @PostMapping("/api/v1/internal/coupons/validate")
    CouponValidationResponse validateCoupon(@RequestBody ValidateCouponRequest req);

    @PostMapping("/api/v1/internal/coupons/redeem")
    CouponValidationResponse redeemCoupon(
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody RedeemCouponRequest req
    );
}
