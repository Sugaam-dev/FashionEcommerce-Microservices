package com.pmrgsolution.authservice.controller;

import com.pmrgsolution.authservice.dto.response.AddressResponse;
import com.pmrgsolution.authservice.dto.response.UserSummaryResponse;
import com.pmrgsolution.authservice.service.AddressService;
import com.pmrgsolution.authservice.service.AdminUserService;
import com.pmrgsolution.shared.core.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/internal/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('INTERNAL_SERVICE')")
@Tag(name = "Internal User Verification", description = "Endpoints for inter-service validation across microservices")
public class InternalAuthController {

    private final AdminUserService adminUserService;
    private final AddressService addressService;

    @GetMapping("/{userId}")
    @Operation(summary = "Get user summary for internal service", description = "Allows downstream services to fetch verified user info via service key")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> getInternalUserById(@PathVariable UUID userId) {
        UserSummaryResponse response = adminUserService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.ok("User retrieved for internal service.", response));
    }

    @GetMapping("/{userId}/addresses/{addressId}")
    @Operation(summary = "Get user address by id for internal service", description = "Allows order-service to validate shipping address via internal service key")
    public ResponseEntity<ApiResponse<AddressResponse>> getInternalAddressById(
            @PathVariable UUID userId,
            @PathVariable UUID addressId
    ) {
        AddressResponse response = addressService.getAddressById(userId, addressId);
        return ResponseEntity.ok(ApiResponse.ok("Address retrieved for internal service.", response));
    }
}
