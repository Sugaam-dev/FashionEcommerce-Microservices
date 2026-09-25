package com.pmrgsolution.orderservice.dto.response;

import java.math.BigDecimal;

public record AdminDashboardStatsResponse(
        long totalOrders,
        BigDecimal totalRevenue,
        long pendingPaymentOrders,
        long processingOrders,
        long deliveredOrders,
        long cancelledOrders,
        long abandonedCartCount,
        BigDecimal revenueToday
) {}
