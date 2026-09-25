package com.pmrgsolution.orderservice.dto.response;

public record AddressSnapshotResponse(
        String recipientName,
        String recipientPhone,
        String addressLine1,
        String addressLine2,
        String landmark,
        String city,
        String state,
        String postalCode,
        String country
) {}
