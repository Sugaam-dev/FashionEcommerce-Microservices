package com.pmrgsolution.notificationservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EmailTemplateRenderingTest {

    private TemplateEngine templateEngine;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        this.templateEngine = engine;
    }

    @Test
    @DisplayName("Should render mail/customer/otp-verification template correctly")
    void testOtpVerificationTemplate() {
        Context context = new Context(Locale.ENGLISH);
        context.setVariables(Map.of(
                "name", "Alex Morgan",
                "otp", "654321",
                "expiryMinutes", 10,
                "emailSubject", "Your OTP Code"
        ));

        String html = templateEngine.process("mail/customer/otp-verification", context);

        assertThat(html).isNotBlank();
        assertThat(html).contains("654321");
        assertThat(html).contains("Alex Morgan");
        assertThat(html).contains("Fashion E-Commerce");
    }

    @Test
    @DisplayName("Should render mail/customer/password-reset template correctly")
    void testPasswordResetTemplate() {
        Context context = new Context(Locale.ENGLISH);
        context.setVariables(Map.of(
                "name", "Alex Morgan",
                "otp", "998877",
                "expiryMinutes", 15
        ));

        String html = templateEngine.process("mail/customer/password-reset", context);

        assertThat(html).isNotBlank();
        assertThat(html).contains("998877");
        assertThat(html).contains("Password Reset Request");
    }

    @Test
    @DisplayName("Should render mail/customer/password-changed template correctly")
    void testPasswordChangedTemplate() {
        Context context = new Context(Locale.ENGLISH);
        context.setVariables(Map.of(
                "firstName", "Alex",
                "changedAt", "2026-10-07 12:00:00 UTC"
        ));

        String html = templateEngine.process("mail/customer/password-changed", context);

        assertThat(html).isNotBlank();
        assertThat(html).contains("Security Alert: Password Changed");
    }

    @Test
    @DisplayName("Should render mail/customer/account-locked template correctly")
    void testAccountLockedTemplate() {
        Context context = new Context(Locale.ENGLISH);
        context.setVariables(Map.of(
                "firstName", "Alex",
                "unlocksAt", "In 30 minutes"
        ));

        String html = templateEngine.process("mail/customer/account-locked", context);

        assertThat(html).isNotBlank();
        assertThat(html).contains("Account Temporarily Locked");
    }

    @Test
    @DisplayName("Should render mail/admin/new-order-alert template correctly")
    void testNewOrderAlertTemplate() {
        Context context = new Context(Locale.ENGLISH);
        context.setVariables(Map.of(
                "orderNumber", "ORD-12345",
                "customerName", "Alex Morgan",
                "total", "4999.00",
                "paymentMethod", "CARD",
                "itemCount", 3
        ));

        String html = templateEngine.process("mail/admin/new-order-alert", context);

        assertThat(html).isNotBlank();
        assertThat(html).contains("ORD-12345");
        assertThat(html).contains("4999.00");
    }

    @Test
    @DisplayName("Should render mail/admin/low-stock-alert template correctly")
    void testLowStockAlertTemplate() {
        Context context = new Context(Locale.ENGLISH);
        context.setVariables(Map.of(
                "productName", "Silk Designer Dress",
                "sku", "DRS-RED-M",
                "currentStock", 2,
                "threshold", 5
        ));

        String html = templateEngine.process("mail/admin/low-stock-alert", context);

        assertThat(html).isNotBlank();
        assertThat(html).contains("DRS-RED-M");
        assertThat(html).contains("Silk Designer Dress");
    }
}
