package com.pmrgsolution.orderservice.feign;

import com.pmrgsolution.orderservice.dto.response.AddressResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "auth-service", url = "${feign.auth-service.url:http://localhost:8081}")
public interface AuthClient {

    @GetMapping("/api/v1/account/addresses/{id}")
    AddressResponse getAddress(
            @RequestHeader("X-Internal-Service-Key") String key,
            @PathVariable("id") UUID id,
            @RequestParam("userId") UUID userId
    );
}
