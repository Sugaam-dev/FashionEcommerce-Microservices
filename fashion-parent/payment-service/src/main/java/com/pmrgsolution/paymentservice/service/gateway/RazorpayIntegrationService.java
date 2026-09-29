package com.pmrgsolution.paymentservice.service.gateway;

import com.pmrgsolution.shared.core.exception.BadRequestException;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Slf4j
@Service
public class RazorpayIntegrationService {

    private final String keyId;
    private final String keySecret;

    public RazorpayIntegrationService(
            @Value("${razorpay.key-id:rzp_test_public_key}") String keyId,
            @Value("${razorpay.key-secret:rzp_test_secret_key_change_in_production}") String keySecret
    ) {
        this.keyId = keyId;
        this.keySecret = keySecret;
    }

    public String getKeyId() {
        return keyId;
    }

    public String createOrder(BigDecimal amount, String currency, String receiptId) {
        log.info("Creating Razorpay order for receipt: {}, amount: {} {}", receiptId, amount, currency);
        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            JSONObject orderRequest = new JSONObject();
            // Convert to smallest currency unit (paise for INR)
            int amountInPaise = amount.multiply(BigDecimal.valueOf(100)).intValue();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", currency != null ? currency : "INR");
            orderRequest.put("receipt", receiptId);
            orderRequest.put("payment_capture", 1);

            Order order = client.orders.create(orderRequest);
            String razorpayOrderId = order.get("id");
            log.info("Razorpay order created successfully: {}", razorpayOrderId);
            return razorpayOrderId;
        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order for receipt {}: {}", receiptId, e.getMessage(), e);
            throw new BadRequestException("Unable to initiate payment with payment gateway: " + e.getMessage());
        }
    }

    public boolean verifySignature(String gatewayOrderId, String gatewayPaymentId, String signature) {
        if (gatewayOrderId == null || gatewayPaymentId == null || signature == null) {
            return false;
        }
        try {
            String payload = gatewayOrderId + "|" + gatewayPaymentId;
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKeySpec);

            byte[] hash = sha256Hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = HexFormat.of().formatHex(hash);

            // Constant-time comparison to protect against timing attacks
            return MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.error("Error during HMAC signature verification: {}", e.getMessage(), e);
            return false;
        }
    }
}
