package com.pmrgsolution.catalogservice.controller;

import com.pmrgsolution.catalogservice.dto.response.ProductDetailResponse;
import com.pmrgsolution.catalogservice.dto.response.VariantResponse;
import com.pmrgsolution.catalogservice.entity.ProductVariant;
import com.pmrgsolution.catalogservice.mapper.ProductVariantMapper;
import com.pmrgsolution.catalogservice.repository.ProductVariantRepository;
import com.pmrgsolution.catalogservice.service.ProductService;
import com.pmrgsolution.shared.core.exception.ResourceNotFoundException;
import com.pmrgsolution.shared.core.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/catalog")
@RequiredArgsConstructor
@Tag(name = "Internal Catalog", description = "Internal service-to-service endpoints for Order and Inventory services")
public class InternalCatalogController {

    private final ProductVariantRepository productVariantRepository;
    private final ProductService productService;

    @GetMapping("/variants/sku/{sku}")
    @Operation(summary = "Find product variant by SKU")
    public ResponseEntity<ApiResponse<VariantResponse>> getVariantBySku(@PathVariable String sku) {
        ProductVariant variant = productVariantRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found with SKU: " + sku));

        return ResponseEntity.ok(ApiResponse.ok(
                "Variant found",
                ProductVariantMapper.toResponse(variant)
        ));
    }

    @GetMapping("/variants/{id}")
    @Operation(summary = "Find product variant by ID")
    public ResponseEntity<ApiResponse<VariantResponse>> getVariantById(@PathVariable UUID id) {
        ProductVariant variant = productVariantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found with ID: " + id));

        return ResponseEntity.ok(ApiResponse.ok(
                "Variant found",
                ProductVariantMapper.toResponse(variant)
        ));
    }

    @GetMapping("/products/{id}")
    @Operation(summary = "Find product details by ID for internal validation")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductById(@PathVariable UUID id) {
        ProductDetailResponse product = productService.getById(id);
        return ResponseEntity.ok(ApiResponse.ok("Product found", product));
    }
}
