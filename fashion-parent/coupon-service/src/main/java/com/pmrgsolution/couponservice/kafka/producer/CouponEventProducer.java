package com.pmrgsolution.couponservice.kafka.producer;

import com.pmrgsolution.couponservice.entity.Coupon;
import com.pmrgsolution.couponservice.entity.CouponUsage;
import com.pmrgsolution.shared.core.constants.KafkaTopics;
import com.pmrgsolution.shared.events.coupon.CouponRedeemedEvent;
import com.pmrgsolution.shared.events.payment.EntityType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponEventProducer {

    private final OutboxEventPublisher outboxEventPublisher;

    public void publishCouponRedeemed(Coupon coupon, CouponUsage usage) {
        CouponRedeemedEvent event = new CouponRedeemedEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                coupon.getId().toString(),
                coupon.getCode(),
                usage.getUserId().toString(),
                EntityType.ORDER,
                usage.getOrderId(),
                usage.getDiscountAmount()
        );

        outboxEventPublisher.saveEvent(
                "Coupon",
                coupon.getId().toString(),
                KafkaTopics.COUPON_REDEEMED,
                event
        );

        log.info("Staged outbox event CouponRedeemedEvent for coupon={}, orderId={}, discountAmount={}",
                coupon.getCode(), usage.getOrderId(), usage.getDiscountAmount());
    }
}
