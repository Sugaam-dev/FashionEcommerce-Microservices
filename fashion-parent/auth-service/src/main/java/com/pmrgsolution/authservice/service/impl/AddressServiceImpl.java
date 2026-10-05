package com.pmrgsolution.authservice.service.impl;

import com.pmrgsolution.authservice.dto.request.AddressRequest;
import com.pmrgsolution.authservice.dto.response.AddressResponse;
import com.pmrgsolution.authservice.entity.User;
import com.pmrgsolution.authservice.entity.UserAddress;
import com.pmrgsolution.authservice.mapper.UserAddressMapper;
import com.pmrgsolution.authservice.repository.UserAddressRepository;
import com.pmrgsolution.authservice.repository.UserRepository;
import com.pmrgsolution.authservice.service.AddressService;
import com.pmrgsolution.shared.core.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final UserAddressRepository userAddressRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getAddresses(UUID userId) {
        return userAddressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId).stream()
                .map(UserAddressMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AddressResponse getAddressById(UUID userId, UUID addressId) {
        UserAddress address = userAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId));

        return UserAddressMapper.toResponse(address);
    }

    @Override
    @Transactional
    public AddressResponse createAddress(UUID userId, AddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        long existingCount = userAddressRepository.countByUserId(userId);
        boolean shouldBeDefault = request.isDefault() || existingCount == 0;

        if (shouldBeDefault && existingCount > 0) {
            userAddressRepository.clearAllDefaultForUser(userId);
        }

        UserAddress address = UserAddressMapper.toEntity(user, request);
        address.setDefault(shouldBeDefault);

        UserAddress saved = userAddressRepository.save(address);
        return UserAddressMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(UUID userId, UUID addressId, AddressRequest request) {
        UserAddress address = userAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId));

        if (request.isDefault()) {
            userAddressRepository.clearAllDefaultForUser(userId);
            address.setDefault(true);
        } else {
            address.setDefault(false);
        }

        address.setAddressType(request.addressType());
        address.setRecipientName(request.recipientName());
        address.setRecipientPhone(request.recipientPhone());
        address.setAddressLine1(request.addressLine1());
        address.setAddressLine2(request.addressLine2());
        address.setLandmark(request.landmark());
        address.setCity(request.city());
        address.setState(request.state());
        address.setPostalCode(request.postalCode());
        if (request.country() != null) {
            address.setCountry(request.country());
        }

        UserAddress saved = userAddressRepository.save(address);
        return UserAddressMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteAddress(UUID userId, UUID addressId) {
        UserAddress address = userAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId));

        userAddressRepository.delete(address);
    }

    @Override
    @Transactional
    public AddressResponse setDefaultAddress(UUID userId, UUID addressId) {
        UserAddress address = userAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId));

        userAddressRepository.clearAllDefaultForUser(userId);
        address.setDefault(true);
        UserAddress saved = userAddressRepository.save(address);

        return UserAddressMapper.toResponse(saved);
    }
}
