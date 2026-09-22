package com.pmrgsolution.shared.events.payment;

/**
 * The type of entity a payment or coupon is associated with.
 * <p>
 * Using an enum instead of a free-form {@code String} prevents silent bugs
 * caused by mismatched literal values across producer and consumer services.
 * Jackson serialises this as {@code "ORDER"}, {@code "SUBSCRIPTION"}, etc. by default.
 */
public enum EntityType {
    ORDER,
    SUBSCRIPTION,
    GIFT_CARD
}
