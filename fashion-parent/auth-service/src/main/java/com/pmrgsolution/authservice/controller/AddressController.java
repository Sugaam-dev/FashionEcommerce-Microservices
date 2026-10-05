package com.pmrgsolution.authservice.controller;

import com.pmrgsolution.authservice.dto.request.AddressRequest;
import com.pmrgsolution.authservice.dto.response.AddressResponse;
import com.pmrgsolution.authservice.security.UserPrincipal;
import com.pmrgsolution.authservice.service.AddressService;
import com.pmrgsolution.shared.core.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/account/addresses")
@RequiredArgsConstructor
@Tag(name = "Address Management", description = "Endpoints for customer address book operations")
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    @Operation(summary = "List saved addresses", description = "Retrieves all saved addresses for the current user")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddresses(@AuthenticationPrincipal UserPrincipal principal) {
        List<AddressResponse> addresses = addressService.getAddresses(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Addresses retrieved successfully.", addresses));
    }

    @GetMapping("/{addressId}")
    @Operation(summary = "Get address details", description = "Retrieves a single address by its UUID")
    public ResponseEntity<ApiResponse<AddressResponse>> getAddressById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID addressId,
            @RequestParam(value = "userId", required = false) UUID queryUserId
    ) {
        UUID effectiveUserId = (principal != null) ? principal.getId() : queryUserId;
        if (effectiveUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        AddressResponse address = addressService.getAddressById(effectiveUserId, addressId);
        return ResponseEntity.ok(ApiResponse.ok("Address retrieved successfully.", address));
    }

    @PostMapping
    @Operation(summary = "Add a new address", description = "Adds a shipping/billing address to the user's address book")
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddressRequest request
    ) {
        AddressResponse address = addressService.createAddress(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Address created successfully.", address));
    }

    @PutMapping("/{addressId}")
    @Operation(summary = "Update an existing address", description = "Modifies an existing address in the user's address book")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID addressId,
            @Valid @RequestBody AddressRequest request
    ) {
        AddressResponse address = addressService.updateAddress(principal.getId(), addressId, request);
        return ResponseEntity.ok(ApiResponse.ok("Address updated successfully.", address));
    }

    @DeleteMapping("/{addressId}")
    @Operation(summary = "Delete an address", description = "Removes an address from the user's address book")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID addressId
    ) {
        addressService.deleteAddress(principal.getId(), addressId);
        return ResponseEntity.ok(ApiResponse.ok("Address deleted successfully.", null));
    }

    @PatchMapping("/{addressId}/default")
    @Operation(summary = "Set address as default", description = "Sets the specified address as default and unsets others")
    public ResponseEntity<ApiResponse<AddressResponse>> setDefaultAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID addressId
    ) {
        AddressResponse address = addressService.setDefaultAddress(principal.getId(), addressId);
        return ResponseEntity.ok(ApiResponse.ok("Address set as default successfully.", address));
    }
}
