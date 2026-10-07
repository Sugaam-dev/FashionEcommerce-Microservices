package com.pmrgsolution.notificationservice.controller;

import com.pmrgsolution.notificationservice.dto.response.StoreInfoSettingsResponse;
import com.pmrgsolution.notificationservice.service.StoreInfoService;
import com.pmrgsolution.shared.core.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/store-info")
@Tag(name = "Store Info", description = "Public store information, announcements, and policy settings")
public class StoreInfoController {

    private final StoreInfoService storeInfoService;

    public StoreInfoController(StoreInfoService storeInfoService) {
        this.storeInfoService = storeInfoService;
    }

    @GetMapping
    @Operation(summary = "Get public store information and active announcements")
    public ResponseEntity<ApiResponse<StoreInfoSettingsResponse>> getStoreInfo(HttpServletRequest httpServletRequest) {
        StoreInfoSettingsResponse response = storeInfoService.getStoreInfo();
        return ResponseEntity.ok(ApiResponse.success(
                200,
                "Store info retrieved successfully.",
                response,
                httpServletRequest.getRequestURI()
        ));
    }
}
