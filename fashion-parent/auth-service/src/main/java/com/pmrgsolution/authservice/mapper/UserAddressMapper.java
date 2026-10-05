package com.pmrgsolution.authservice.mapper;

import com.pmrgsolution.authservice.dto.request.AddressRequest;
import com.pmrgsolution.authservice.dto.response.AddressResponse;
import com.pmrgsolution.authservice.entity.User;
import com.pmrgsolution.authservice.entity.UserAddress;

public final class UserAddressMapper {

    private UserAddressMapper() {}

    public static AddressResponse toResponse(UserAddress address) {
        if (address == null) {
            return null;
        }

        return new AddressResponse(
                address.getId(),
                address.getUser() != null ? address.getUser().getId() : null,
                address.getAddressType(),
                address.getRecipientName(),
                address.getRecipientPhone(),
                address.getAddressLine1(),
                address.getAddressLine2(),
                address.getLandmark(),
                address.getCity(),
                address.getState(),
                address.getPostalCode(),
                address.getCountry(),
                address.isDefault(),
                address.getCreatedAt()
        );
    }

    public static UserAddress toEntity(User user, AddressRequest request) {
        return UserAddress.builder()
                .user(user)
                .addressType(request.addressType())
                .recipientName(request.recipientName())
                .recipientPhone(request.recipientPhone())
                .addressLine1(request.addressLine1())
                .addressLine2(request.addressLine2())
                .landmark(request.landmark())
                .city(request.city())
                .state(request.state())
                .postalCode(request.postalCode())
                .country(request.country() != null ? request.country() : "IN")
                .isDefault(Boolean.TRUE.equals(request.isDefault()))
                .build();
    }
}
