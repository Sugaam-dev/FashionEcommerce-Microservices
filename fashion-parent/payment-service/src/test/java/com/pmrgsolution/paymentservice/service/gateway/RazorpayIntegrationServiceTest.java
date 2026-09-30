package com.pmrgsolution.paymentservice.service.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

class RazorpayIntegrationServiceTest {

    private RazorpayIntegrationService razorpayService;
    private final String testKeyId = "rzp_test_key123";
    private final String testKeySecret = "my_super_secret_test_key_1234567890";

    @BeforeEach
    void setUp() {
        razorpayService = new RazorpayIntegrationService(testKeyId, testKeySecret);
    }

    private String generateSignature(String orderId, String paymentId, String secret) throws Exception {
        String payload = orderId + "|" + paymentId;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("verifySignature with correct payload returns true")
    void verifySignature_withCorrectPayload_returnsTrue() throws Exception {
        String orderId = "order_O8p123456789";
        String paymentId = "pay_P8p987654321";
        String validSignature = generateSignature(orderId, paymentId, testKeySecret);

        boolean result = razorpayService.verifySignature(orderId, paymentId, validSignature);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("verifySignature with tampered signature returns false")
    void verifySignature_withTamperedPayload_returnsFalse() throws Exception {
        String orderId = "order_O8p123456789";
        String paymentId = "pay_P8p987654321";
        String validSignature = generateSignature(orderId, paymentId, testKeySecret);
        String tamperedSignature = validSignature.substring(0, validSignature.length() - 2) + "ab";

        boolean result = razorpayService.verifySignature(orderId, paymentId, tamperedSignature);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("verifySignature with null inputs returns false safely")
    void verifySignature_withNullInputs_returnsFalseSafely() {
        assertThat(razorpayService.verifySignature(null, "pay_123", "sig_123")).isFalse();
        assertThat(razorpayService.verifySignature("order_123", null, "sig_123")).isFalse();
        assertThat(razorpayService.verifySignature("order_123", "pay_123", null)).isFalse();
    }
}
