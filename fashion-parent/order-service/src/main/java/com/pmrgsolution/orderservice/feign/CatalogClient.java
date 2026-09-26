package com.pmrgsolution.orderservice.feign;

import com.pmrgsolution.orderservice.dto.response.ProductDetailResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "catalog-service", url = "${feign.catalog-service.url:http://localhost:8082}")
public interface CatalogClient {

    @GetMapping("/api/v1/products/{id}")
    ProductDetailResponse getProductById(@PathVariable("id") UUID id);
}
