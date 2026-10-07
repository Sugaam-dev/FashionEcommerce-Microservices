package com.pmrgsolution.notificationservice;

import com.pmrgsolution.notificationservice.entity.EmailUsageQuota;
import com.pmrgsolution.notificationservice.entity.NotificationLog;
import com.pmrgsolution.notificationservice.enums.NotificationStatus;
import com.pmrgsolution.notificationservice.repository.EmailUsageQuotaRepository;
import com.pmrgsolution.notificationservice.repository.NotificationLogRepository;
import com.pmrgsolution.notificationservice.service.EmailSenderServiceImpl;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailSenderServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private SpringTemplateEngine templateEngine;

    @Mock
    private NotificationLogRepository notificationLogRepository;

    @Mock
    private EmailUsageQuotaRepository emailUsageQuotaRepository;

    @InjectMocks
    private EmailSenderServiceImpl emailSenderService;

    private String todayKey;

    @BeforeEach
    void setUp() {
        todayKey = LocalDate.now(ZoneOffset.UTC).toString();
        ReflectionTestUtils.setField(emailSenderService, "fromAddress", "noreply@fashionecommerce.com");
        ReflectionTestUtils.setField(emailSenderService, "fromName", "Fashion E-Commerce");
        ReflectionTestUtils.setField(emailSenderService, "defaultDailyLimit", 3000);
    }

    @Test
    @DisplayName("Should suppress email when daily quota limit is reached")
    void sendEmail_WhenQuotaExceeded_ShouldSuppressNotification() {
        EmailUsageQuota quota = EmailUsageQuota.builder()
                .dateKey(todayKey)
                .count(3000)
                .dailyLimit(3000)
                .lastSentAt(Instant.now())
                .build();

        when(emailUsageQuotaRepository.findByDateKey(todayKey)).thenReturn(Optional.of(quota));

        emailSenderService.sendEmail("user@example.com", "Test Subject", "customer/otp-verification", Map.of("otp", "123456"));

        verify(mailSender, never()).send(any(MimeMessage.class));

        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(logCaptor.capture());

        NotificationLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getStatus()).isEqualTo(NotificationStatus.SUPPRESSED);
        assertThat(savedLog.getRecipient()).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("Should send email successfully and increment quota when within limit")
    void sendEmail_WhenQuotaAvailable_ShouldSendAndIncrementQuota() {
        EmailUsageQuota quota = EmailUsageQuota.builder()
                .dateKey(todayKey)
                .count(10)
                .dailyLimit(3000)
                .lastSentAt(Instant.now())
                .build();

        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));

        when(emailUsageQuotaRepository.findByDateKey(todayKey)).thenReturn(Optional.of(quota));
        when(templateEngine.process(eq("mail/customer/otp-verification"), any(Context.class))).thenReturn("<html>OTP: 123456</html>");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailSenderService.sendEmail("user@example.com", "Your OTP", "customer/otp-verification", Map.of("otp", "123456"));

        verify(mailSender).send(mimeMessage);
        verify(emailUsageQuotaRepository).save(quota);
        assertThat(quota.getCount()).isEqualTo(11);

        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(logCaptor.capture());

        NotificationLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(savedLog.getSentAt()).isNotNull();
    }
}
