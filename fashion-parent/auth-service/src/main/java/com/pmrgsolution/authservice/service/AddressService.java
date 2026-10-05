package com.pmrgsolution.authservice.service;

import com.pmrgsolution.authservice.dto.request.AddressRequest;
import com.pmrgsolution.authservice.dto.response.AddressResponse;

import java.util.List;
import java.util.UUID;

public interface AddressService {

    List<AddressResponse> getAddresses(UUID userId);

    AddressResponse getAddressById(UUID userId, UUID addressId);

    AddressResponse createAddress(UUID userId, AddressRequest request);

    AddressResponse updateAddress(UUID userId, UUID addressId, AddressRequest request);

    void deleteAddress(UUID userId, UUID addressId);

    AddressResponse setDefaultAddress(UUID userId, UUID addressId);
}
