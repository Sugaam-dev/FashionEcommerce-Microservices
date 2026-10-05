package com.pmrgsolution.authservice.dto.response;

import com.pmrgsolution.authservice.enums.AddressType;

import java.time.Instant;
import java.util.UUID;

public record AddressResponse(
        UUID id,
        UUID userId,
        AddressType addressType,
        String recipientName,
        String recipientPhone,
        String addressLine1,
        String addressLine2,
        String landmark,
        String city,
        String state,
        String postalCode,
        String country,
        boolean isDefault,
        Instant createdAt
) {
    public AddressResponse(UUID id, AddressType addressType, String recipientName, String recipientPhone,
                           String addressLine1, String addressLine2, String landmark, String city,
                           String state, String postalCode, String country, boolean isDefault, Instant createdAt) {
        this(id, null, addressType, recipientName, recipientPhone, addressLine1, addressLine2, landmark, city, state, postalCode, country, isDefault, createdAt);
    }
}
