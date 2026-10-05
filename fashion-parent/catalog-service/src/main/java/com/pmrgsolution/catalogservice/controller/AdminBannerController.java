package com.pmrgsolution.catalogservice.controller;

import com.pmrgsolution.catalogservice.dto.request.StoreBannerRequest;
import com.pmrgsolution.catalogservice.dto.response.StoreBannerResponse;
import com.pmrgsolution.catalogservice.service.BannerService;
import com.pmrgsolution.shared.core.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/banners")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin Banners", description = "Admin endpoints for managing promotional store banners")
public class AdminBannerController {

    private final BannerService bannerService;

    @GetMapping
    @Operation(summary = "Get all store banners")
    public ResponseEntity<ApiResponse<List<StoreBannerResponse>>> getAllBanners() {
        List<StoreBannerResponse> banners = bannerService.getAllBanners();
        return ResponseEntity.ok(ApiResponse.ok("All banners retrieved", banners));
    }

    @PostMapping
    @Operation(summary = "Create a store banner")
    public ResponseEntity<ApiResponse<StoreBannerResponse>> createBanner(
            @Valid @RequestBody StoreBannerRequest request
    ) {
        StoreBannerResponse banner = bannerService.createBanner(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Store banner created successfully", banner));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a store banner")
    public ResponseEntity<ApiResponse<StoreBannerResponse>> updateBanner(
            @PathVariable UUID id,
            @Valid @RequestBody StoreBannerRequest request
    ) {
        StoreBannerResponse banner = bannerService.updateBanner(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Store banner updated successfully", banner));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a store banner")
    public ResponseEntity<ApiResponse<Void>> deleteBanner(@PathVariable UUID id) {
        bannerService.deleteBanner(id);
        return ResponseEntity.ok(ApiResponse.ok("Store banner deleted successfully", null));
    }

    @PatchMapping("/{id}/toggle")
    @Operation(summary = "Toggle store banner active status")
    public ResponseEntity<ApiResponse<StoreBannerResponse>> toggleBanner(@PathVariable UUID id) {
        StoreBannerResponse banner = bannerService.toggleBannerStatus(id);
        return ResponseEntity.ok(ApiResponse.ok("Banner active status toggled", banner));
    }
}
