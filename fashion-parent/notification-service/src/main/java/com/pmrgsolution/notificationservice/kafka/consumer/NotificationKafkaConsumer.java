package com.pmrgsolution.notificationservice.kafka.consumer;

import com.pmrgsolution.shared.core.constants.KafkaTopics;
import com.pmrgsolution.notificationservice.service.EmailSenderService;
import com.pmrgsolution.notificationservice.service.SseHubService;
import com.pmrgsolution.shared.core.util.MaskingUtil;
import com.pmrgsolution.shared.events.inventory.InventoryLowEvent;
import com.pmrgsolution.shared.events.notification.NotificationRequestEvent;
import com.pmrgsolution.shared.events.order.OrderCreatedEvent;
import com.pmrgsolution.shared.events.payment.PaymentVerifiedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class NotificationKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationKafkaConsumer.class);

    private final EmailSenderService emailSenderService;
    private final SseHubService sseHubService;

    @Value("${app.admin.notification-email:admin@fashionecommerce.com}")
    private String adminNotificationEmail;

    public NotificationKafkaConsumer(EmailSenderService emailSenderService, SseHubService sseHubService) {
        this.emailSenderService = emailSenderService;
        this.sseHubService = sseHubService;
    }

    @KafkaListener(
            topics = KafkaTopics.NOTIFICATION_REQUEST,
            groupId = "${spring.kafka.consumer.group-id:notification-service-group}"
    )
    public void handleNotificationRequest(NotificationRequestEvent event) {
        log.info("Received NotificationRequestEvent for recipient: {} [template: {}]",
                MaskingUtil.maskEmail(event.recipient()), event.templateId());

        String template = resolveTemplatePath(event.templateId());
        String subject = resolveSubject(template, event.variables());
        emailSenderService.sendEmail(event.recipient(), subject, template, event.variables());
    }

    private String resolveTemplatePath(String templateId) {
        if (templateId == null) {
            return "customer/otp-verification";
        }
        return switch (templateId) {
            case "OTP_REGISTRATION", "otp-verification" -> "customer/otp-verification";
            case "OTP_PASSWORD_RESET", "password-reset" -> "customer/password-reset";
            case "password-changed" -> "customer/password-changed";
            case "order-confirmation" -> "customer/order-confirmation";
            case "order-shipped" -> "customer/order-shipped";
            case "order-delivered" -> "customer/order-delivered";
            case "order-cancelled" -> "customer/order-cancelled";
            case "payment-proof-received" -> "customer/payment-proof-received";
            case "account-locked" -> "customer/account-locked";
            case "new-order-alert" -> "admin/new-order-alert";
            case "new-inquiry-alert" -> "admin/new-inquiry-alert";
            case "manual-payment-alert" -> "admin/manual-payment-alert";
            case "vip-order-alert" -> "admin/vip-order-alert";
            case "payment-approved-alert" -> "admin/payment-approved-alert";
            case "low-stock-alert" -> "admin/low-stock-alert";
            default -> templateId;
        };
    }

    @KafkaListener(
            topics = KafkaTopics.ORDER_CREATED,
            groupId = "${spring.kafka.consumer.group-id:notification-service-group}"
    )
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent for order: {}", event.orderId());

        // 1. Alert Admin via Email
        Map<String, Object> adminVars = Map.of(
                "orderNumber", event.orderId(),
                "customerName", "Customer (" + event.userId() + ")",
                "total", event.totalAmount() != null ? event.totalAmount().toString() : "0.00",
                "paymentMethod", "ONLINE",
                "itemCount", event.items() != null ? event.items().size() : 0
        );

        emailSenderService.sendEmail(
                adminNotificationEmail,
                "New Order: #" + event.orderId(),
                "admin/new-order-alert",
                adminVars
        );

        // 2. Broadcast to Admin Dashboard via SSE
        sseHubService.broadcast("NEW_ORDER", event);
    }

    @KafkaListener(
            topics = KafkaTopics.PAYMENT_VERIFIED,
            groupId = "${spring.kafka.consumer.group-id:notification-service-group}"
    )
    public void handlePaymentVerified(PaymentVerifiedEvent event) {
        log.info("Received PaymentVerifiedEvent for payment: {} [entity: {}]", event.paymentId(), event.entityId());

        Map<String, Object> vars = Map.of(
                "orderNumber", event.entityId(),
                "customerName", "Customer",
                "amount", "N/A",
                "approvedBy", event.verifiedBy() != null ? event.verifiedBy() : "System"
        );

        emailSenderService.sendEmail(
                adminNotificationEmail,
                "Payment Approved: #" + event.entityId(),
                "admin/payment-approved-alert",
                vars
        );

        sseHubService.broadcast("PAYMENT_VERIFIED", event);
    }

    @KafkaListener(
            topics = KafkaTopics.INVENTORY_LOW_STOCK,
            groupId = "${spring.kafka.consumer.group-id:notification-service-group}"
    )
    public void handleInventoryLowStock(InventoryLowEvent event) {
        log.info("Received InventoryLowEvent for variant: {} [currentStock: {}]",
                event.variantId(), event.currentStock());

        Map<String, Object> vars = Map.of(
                "productName", event.productName(),
                "sku", event.variantId(),
                "currentStock", event.currentStock(),
                "threshold", event.threshold()
        );

        emailSenderService.sendEmail(
                adminNotificationEmail,
                "⚠️ Low Stock Alert: " + event.productName(),
                "admin/low-stock-alert",
                vars
        );

        sseHubService.broadcast("LOW_STOCK", event);
    }

    private String resolveSubject(String templateId, Map<String, Object> variables) {
        if (variables != null && variables.containsKey("subject")) {
            return String.valueOf(variables.get("subject"));
        }

        if (templateId == null) {
            return "Notification Alert";
        }

        return switch (templateId) {
            case "customer/otp-verification", "otp-verification" ->
                    "Your Verification Code: " + (variables != null ? variables.getOrDefault("otp", "") : "");
            case "customer/password-reset", "password-reset" -> "Password Reset OTP";
            case "customer/password-changed", "password-changed" -> "Security Alert: Password Changed";
            case "customer/order-confirmation", "order-confirmation" ->
                    "Order Confirmed: #" + (variables != null ? variables.getOrDefault("orderNumber", "") : "");
            case "customer/order-shipped", "order-shipped" ->
                    "Your Order is Shipped! #" + (variables != null ? variables.getOrDefault("orderNumber", "") : "");
            case "customer/order-delivered", "order-delivered" ->
                    "Delivered! #" + (variables != null ? variables.getOrDefault("orderNumber", "") : "");
            case "customer/order-cancelled", "order-cancelled" ->
                    "Order Cancelled: #" + (variables != null ? variables.getOrDefault("orderNumber", "") : "");
            case "customer/payment-proof-received", "payment-proof-received" ->
                    "Payment Proof Received – Pending Review";
            case "customer/account-locked", "account-locked" ->
                    "Security Alert: Your Account Has Been Temporarily Locked";
            case "admin/new-order-alert", "new-order-alert" ->
                    "New Order: #" + (variables != null ? variables.getOrDefault("orderNumber", "") : "");
            case "admin/new-inquiry-alert", "new-inquiry-alert" ->
                    "New Customer Inquiry: " + (variables != null ? variables.getOrDefault("subject", "") : "");
            case "admin/manual-payment-alert", "manual-payment-alert" ->
                    "Manual Payment Proof Submitted – Review Required";
            case "admin/vip-order-alert", "vip-order-alert" ->
                    "⭐ VIP Order Alert: #" + (variables != null ? variables.getOrDefault("orderNumber", "") : "");
            case "admin/payment-approved-alert", "payment-approved-alert" ->
                    "Payment Approved: #" + (variables != null ? variables.getOrDefault("orderNumber", "") : "");
            case "admin/low-stock-alert", "low-stock-alert" ->
                    "⚠️ Low Stock Alert: " + (variables != null ? variables.getOrDefault("sku", "") : "");
            default -> "Notification Alert: " + templateId;
        };
    }
}
