package com.pmrgsolution.shared.core.constants;

/**
 * Centralised Kafka topic name registry.
 * <p>
 * All topics follow the pattern: {@code {domain}.{event-name}.{version}}
 * Versioning allows non-breaking schema evolution by introducing a new topic
 * (e.g. {@code order.created.v2}) while consumers migrate incrementally.
 */
public final class KafkaTopics {

    // ─── Auth ─────────────────────────────────────────────────────────────────
    public static final String USER_REGISTERED       = "auth.user-registered.v1";

    // ─── Order ────────────────────────────────────────────────────────────────
    public static final String ORDER_CREATED         = "order.created.v1";
    public static final String ORDER_CANCELLED       = "order.cancelled.v1";

    // ─── Payment ──────────────────────────────────────────────────────────────
    public static final String PAYMENT_INITIATED     = "payment.initiated.v1";
    public static final String PAYMENT_VERIFIED      = "payment.verified.v1";
    public static final String PAYMENT_REJECTED      = "payment.rejected.v1";

    // ─── Inventory ────────────────────────────────────────────────────────────
    public static final String INVENTORY_LOW         = "inventory.low-stock.v1";
    public static final String INVENTORY_LOW_STOCK   = "inventory.low-stock.v1";

    // ─── Coupon ───────────────────────────────────────────────────────────────
    public static final String COUPON_REDEEMED       = "coupon.redeemed.v1";

    // ─── Notification ─────────────────────────────────────────────────────────
    public static final String NOTIFICATION_REQUEST  = "notification.request.v1";

    private KafkaTopics() {}
}
