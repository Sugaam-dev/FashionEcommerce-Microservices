package com.pmrgsolution.catalogservice.controller;

import com.pmrgsolution.catalogservice.dto.request.ProductCreateRequest;
import com.pmrgsolution.catalogservice.dto.request.ProductFilterCriteria;
import com.pmrgsolution.catalogservice.dto.request.ProductUpdateRequest;
import com.pmrgsolution.catalogservice.dto.request.VariantCreateRequest;
import com.pmrgsolution.catalogservice.dto.response.ProductDetailResponse;
import com.pmrgsolution.catalogservice.dto.response.ProductSummaryResponse;
import com.pmrgsolution.catalogservice.dto.response.VariantResponse;
import com.pmrgsolution.catalogservice.enums.ProductStatus;
import com.pmrgsolution.catalogservice.service.ProductService;
import com.pmrgsolution.shared.core.response.ApiResponse;
import com.pmrgsolution.shared.core.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/products")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin Products", description = "Admin endpoints for managing products, variants, and image uploads")
public class AdminProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Search all products including drafts and archived")
    public ResponseEntity<ApiResponse<PageResponse<ProductSummaryResponse>>> getAllProducts(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        ProductFilterCriteria criteria = new ProductFilterCriteria(
                categoryId,
                brand,
                minPrice,
                maxPrice,
                minRating,
                status,
                search
        );
        PageResponse<ProductSummaryResponse> response = productService.getProducts(criteria, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Products retrieved successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product details by ID")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductById(@PathVariable UUID id) {
        ProductDetailResponse product = productService.getById(id);
        return ResponseEntity.ok(ApiResponse.ok("Product details retrieved", product));
    }

    @PostMapping
    @Operation(summary = "Create a new product with initial variants")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> createProduct(
            @Valid @RequestBody ProductCreateRequest request
    ) {
        ProductDetailResponse product = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Product created successfully", product));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update product details")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody ProductUpdateRequest request
    ) {
        ProductDetailResponse updated = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Product updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete / archive product")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.ok("Product archived successfully", null));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update product publishing status")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> updateStatus(
            @PathVariable UUID id,
            @RequestParam ProductStatus status
    ) {
        ProductDetailResponse updated = productService.updateProductStatus(id, status);
        return ResponseEntity.ok(ApiResponse.ok("Product status updated successfully", updated));
    }

    @PostMapping("/{id}/variants")
    @Operation(summary = "Add a new variant to product")
    public ResponseEntity<ApiResponse<VariantResponse>> addVariant(
            @PathVariable UUID id,
            @Valid @RequestBody VariantCreateRequest request
    ) {
        VariantResponse variant = productService.addVariant(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Variant added successfully", variant));
    }

    @PutMapping("/{id}/variants/{variantId}")
    @Operation(summary = "Update an existing variant")
    public ResponseEntity<ApiResponse<VariantResponse>> updateVariant(
            @PathVariable UUID id,
            @PathVariable UUID variantId,
            @Valid @RequestBody VariantCreateRequest request
    ) {
        VariantResponse variant = productService.updateVariant(id, variantId, request);
        return ResponseEntity.ok(ApiResponse.ok("Variant updated successfully", variant));
    }

    @DeleteMapping("/{id}/variants/{variantId}")
    @Operation(summary = "Delete a variant")
    public ResponseEntity<ApiResponse<Void>> deleteVariant(
            @PathVariable UUID id,
            @PathVariable UUID variantId
    ) {
        productService.deleteVariant(id, variantId);
        return ResponseEntity.ok(ApiResponse.ok("Variant deleted successfully", null));
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload product images to S3")
    public ResponseEntity<ApiResponse<List<String>>> uploadImages(
            @PathVariable UUID id,
            @RequestParam("files") List<MultipartFile> files
    ) {
        List<String> imageUrls = productService.uploadProductImages(id, files);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Images uploaded successfully", imageUrls));
    }
}
