package com.pmrgsolution.paymentservice.scheduler;

import com.pmrgsolution.paymentservice.domain.entity.PaymentTransaction;
import com.pmrgsolution.paymentservice.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentProofCleanupScheduler {

    private final PaymentTransactionRepository paymentTransactionRepository;

    @Scheduled(cron = "0 0 3 * * 0") // Every Sunday at 03:00 AM
    @Transactional
    public void cleanupOldProofUrls() {
        Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);
        log.info("Starting scheduled cleanup of manual payment proof URLs older than 30 days (cutoff: {})", cutoff);

        List<PaymentTransaction> transactions = paymentTransactionRepository.findStaleManualProofTransactions(cutoff);

        if (transactions.isEmpty()) {
            log.info("No stale payment proof URLs found for cleanup");
            return;
        }

        for (PaymentTransaction tx : transactions) {
            tx.setManualUpiProofImageUrl(null);
        }

        paymentTransactionRepository.saveAll(transactions);
        log.info("Successfully cleaned up {} manual payment proof URLs", transactions.size());
    }
}
