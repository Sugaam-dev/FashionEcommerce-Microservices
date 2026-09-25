package com.pmrgsolution.orderservice.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderShippingConfigResponse(
        UUID id,
        BigDecimal defaultShippingFee,
        BigDecimal freeShippingThreshold,
        int estimatedDeliveryMinDays,
        int estimatedDeliveryMaxDays
) {}
