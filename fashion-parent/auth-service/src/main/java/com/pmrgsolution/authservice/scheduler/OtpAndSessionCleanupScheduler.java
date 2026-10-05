package com.pmrgsolution.authservice.scheduler;

import com.pmrgsolution.authservice.repository.EmailOtpRepository;
import com.pmrgsolution.authservice.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OtpAndSessionCleanupScheduler {

    private static final int STALE_SESSION_RETENTION_DAYS = 30;

    private final EmailOtpRepository emailOtpRepository;
    private final UserSessionRepository userSessionRepository;

    @Scheduled(cron = "${app.cleanup.cron:0 0 3 * * *}")
    @Transactional
    public void cleanupExpiredData() {
        Instant now = Instant.now();
        Instant sessionCutoff = now.minus(STALE_SESSION_RETENTION_DAYS, ChronoUnit.DAYS);

        log.info("Starting scheduled cleanup of expired OTPs and stale sessions...");

        try {
            int purgedOtps = emailOtpRepository.purgeExpiredAndConsumedOtps(now);
            log.info("Purged {} expired/consumed OTP records", purgedOtps);
        } catch (Exception e) {
            log.error("Failed to purge expired OTP records: {}", e.getMessage());
        }

        try {
            int purgedSessions = userSessionRepository.purgeStaleAndRevoked(now, sessionCutoff);
            log.info("Purged {} expired/stale revoked user session records", purgedSessions);
        } catch (Exception e) {
            log.error("Failed to purge stale user session records: {}", e.getMessage());
        }

        log.info("Scheduled cleanup completed.");
    }
}
