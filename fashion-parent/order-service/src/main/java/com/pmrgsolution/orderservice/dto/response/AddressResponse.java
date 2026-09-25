package com.pmrgsolution.orderservice.dto.response;

import java.util.UUID;

public record AddressResponse(
        UUID id,
        UUID userId,
        String addressType,
        String recipientName,
        String recipientPhone,
        String addressLine1,
        String addressLine2,
        String landmark,
        String city,
        String state,
        String postalCode,
        String country,
        boolean isDefault
) {}
