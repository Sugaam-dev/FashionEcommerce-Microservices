package com.pmrgsolution.shared.core.constants;

public final class KafkaTopics {
    public static final String USER_REGISTERED = "user-registered";
    public static final String NOTIFICATION_REQUEST = "notification-request";
    public static final String PAYMENT_INITIATED = "payment-initiated";
    public static final String PAYMENT_VERIFIED = "payment-verified";
    public static final String PAYMENT_REJECTED = "payment-rejected";
    public static final String ORDER_CREATED = "order-created";
    public static final String ORDER_CANCELLED = "order-cancelled";
    public static final String INVENTORY_LOW = "inventory-low";
    public static final String COUPON_REDEEMED = "coupon-redeemed";

    private KafkaTopics() {}
}
