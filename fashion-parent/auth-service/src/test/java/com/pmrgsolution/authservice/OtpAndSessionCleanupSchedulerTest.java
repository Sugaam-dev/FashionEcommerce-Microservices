package com.pmrgsolution.authservice;

import com.pmrgsolution.authservice.repository.EmailOtpRepository;
import com.pmrgsolution.authservice.repository.UserSessionRepository;
import com.pmrgsolution.authservice.scheduler.OtpAndSessionCleanupScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OtpAndSessionCleanupSchedulerTest {

    @Mock
    private EmailOtpRepository emailOtpRepository;
    @Mock
    private UserSessionRepository userSessionRepository;

    @InjectMocks
    private OtpAndSessionCleanupScheduler cleanupScheduler;

    @Test
    @DisplayName("Cleanup Scheduler: Invokes purge methods on repositories")
    void testCleanupExpiredData_Success() {
        when(emailOtpRepository.purgeExpiredAndConsumedOtps(any(Instant.class))).thenReturn(15);
        when(userSessionRepository.purgeStaleAndRevoked(any(Instant.class), any(Instant.class))).thenReturn(8);

        cleanupScheduler.cleanupExpiredData();

        verify(emailOtpRepository).purgeExpiredAndConsumedOtps(any(Instant.class));
        verify(userSessionRepository).purgeStaleAndRevoked(any(Instant.class), any(Instant.class));
    }
}
