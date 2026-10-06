package com.pmrgsolution.notificationservice.service;

import com.pmrgsolution.notificationservice.entity.EmailUsageQuota;
import com.pmrgsolution.notificationservice.entity.NotificationLog;
import com.pmrgsolution.notificationservice.enums.NotificationStatus;
import com.pmrgsolution.notificationservice.repository.EmailUsageQuotaRepository;
import com.pmrgsolution.notificationservice.repository.NotificationLogRepository;
import com.pmrgsolution.shared.core.util.MaskingUtil;
import com.pmrgsolution.shared.events.notification.NotificationChannel;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Map;

@Service
public class EmailSenderServiceImpl implements EmailSenderService {

    private static final Logger log = LoggerFactory.getLogger(EmailSenderServiceImpl.class);

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final NotificationLogRepository notificationLogRepository;
    private final EmailUsageQuotaRepository emailUsageQuotaRepository;

    @Value("${app.mail.from:noreply@fashionecommerce.com}")
    private String fromAddress;

    @Value("${app.mail.from-name:Fashion E-Commerce}")
    private String fromName;

    @Value("${app.mail.daily-limit:3000}")
    private int defaultDailyLimit;

    public EmailSenderServiceImpl(
            JavaMailSender mailSender,
            SpringTemplateEngine templateEngine,
            NotificationLogRepository notificationLogRepository,
            EmailUsageQuotaRepository emailUsageQuotaRepository
    ) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.notificationLogRepository = notificationLogRepository;
        this.emailUsageQuotaRepository = emailUsageQuotaRepository;
    }

    @Async("taskExecutor")
    @Override
    @Transactional
    public void sendEmail(String to, String subject, String templateId, Map<String, Object> variables) {
        String todayKey = LocalDate.now(ZoneOffset.UTC).toString();

        EmailUsageQuota quota = emailUsageQuotaRepository.findByDateKey(todayKey)
                .orElseGet(() -> emailUsageQuotaRepository.save(
                        EmailUsageQuota.builder()
                                .dateKey(todayKey)
                                .count(0)
                                .dailyLimit(defaultDailyLimit)
                                .lastSentAt(Instant.now())
                                .build()
                ));

        if (quota.getCount() >= quota.getDailyLimit()) {
            log.warn("Daily email quota exceeded for date: {}. Count: {}, Limit: {}. Email to {} SUPPRESSED.",
                    todayKey, quota.getCount(), quota.getDailyLimit(), MaskingUtil.maskEmail(to));

            NotificationLog suppressedLog = NotificationLog.builder()
                    .recipient(to)
                    .channel(NotificationChannel.EMAIL)
                    .templateId(templateId)
                    .subject(subject)
                    .status(NotificationStatus.SUPPRESSED)
                    .errorMessage("Daily quota exceeded: " + quota.getCount() + "/" + quota.getDailyLimit())
                    .retryCount(0)
                    .build();
            notificationLogRepository.save(suppressedLog);
            return;
        }

        try {
            String resolvedTemplate = templateId != null ? templateId : "customer/otp-verification";
            if (!resolvedTemplate.startsWith("mail/")) {
                resolvedTemplate = "mail/" + resolvedTemplate;
            }

            Context context = new Context(Locale.ENGLISH);
            if (variables != null) {
                context.setVariables(variables);
            }

            String htmlBody = templateEngine.process(resolvedTemplate, context);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(mimeMessage);

            quota.setCount(quota.getCount() + 1);
            quota.setLastSentAt(Instant.now());
            emailUsageQuotaRepository.save(quota);

            NotificationLog sentLog = NotificationLog.builder()
                    .recipient(to)
                    .channel(NotificationChannel.EMAIL)
                    .templateId(templateId)
                    .subject(subject)
                    .status(NotificationStatus.SENT)
                    .sentAt(Instant.now())
                    .retryCount(0)
                    .build();
            notificationLogRepository.save(sentLog);

            log.info("Email successfully dispatched to: {} [template: {}]", MaskingUtil.maskEmail(to), templateId);

        } catch (Exception ex) {
            log.error("Failed to deliver email to {}: {}", MaskingUtil.maskEmail(to), ex.getMessage());

            NotificationLog failedLog = NotificationLog.builder()
                    .recipient(to)
                    .channel(NotificationChannel.EMAIL)
                    .templateId(templateId)
                    .subject(subject)
                    .status(NotificationStatus.FAILED)
                    .errorMessage(ex.getMessage())
                    .retryCount(1)
                    .build();
            notificationLogRepository.save(failedLog);
        }
    }
}
