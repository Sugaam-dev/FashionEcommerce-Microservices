package com.pmrgsolution.catalogservice.controller;

import com.pmrgsolution.catalogservice.dto.response.StoreBannerResponse;
import com.pmrgsolution.catalogservice.enums.BannerPosition;
import com.pmrgsolution.catalogservice.service.BannerService;
import com.pmrgsolution.shared.core.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/banners")
@RequiredArgsConstructor
@Tag(name = "Public Banners", description = "Public endpoints for retrieving promotional store banners")
public class PublicBannerController {

    private final BannerService bannerService;

    @GetMapping("/{position}")
    @Operation(summary = "Get active store banners by position")
    public ResponseEntity<ApiResponse<List<StoreBannerResponse>>> getBannersByPosition(
            @PathVariable BannerPosition position
    ) {
        List<StoreBannerResponse> banners = bannerService.getBannersByPosition(position);
        return ResponseEntity.ok(ApiResponse.ok("Store banners retrieved successfully", banners));
    }
}
