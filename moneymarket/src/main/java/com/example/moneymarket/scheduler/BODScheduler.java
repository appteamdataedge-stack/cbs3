package com.example.moneymarket.scheduler;

import com.example.moneymarket.service.BODValueDateService;
import com.example.moneymarket.service.SystemDateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * BOD (Beginning-of-Day) processing service
 * NOTE: This is now a MANUAL operation, not automatic scheduling
 *
 * Call runManualBOD() via controller endpoint to:
 * - Process future-dated transactions whose value date has arrived
 * - Update transaction statuses from Future to Posted
 * - Update account and GL balances for matured future-dated transactions
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BODScheduler {

    private final BODValueDateService bodValueDateService;
    private final SystemDateService systemDateService;

    /**
     * Run BOD processing manually
     * This should be called via API endpoint by administrators
     * This runs BEFORE business operations start for the day
     */
    public BODResult runManualBOD() {
        LocalDate systemDate = systemDateService.getSystemDate();
        log.info("=".repeat(80));
        log.info("Starting MANUAL BOD (Beginning of Day) processing for date: {}", systemDate);
        log.info("=".repeat(80));

        BODResult result = new BODResult();
        result.setSystemDate(systemDate);

        try {
            // Step 1: Check pending future-dated transactions
            long pendingCount = bodValueDateService.getPendingFutureDatedTransactionsCount();
            log.info("BOD: Found {} pending future-dated transactions", pendingCount);
            result.setPendingCountBefore(pendingCount);

            // Step 2: Process future-dated transactions whose value date has arrived
            int processedCount = bodValueDateService.processFutureDatedTransactions();
            result.setProcessedCount(processedCount);

            if (processedCount > 0) {
                log.info("BOD: Successfully processed {} future-dated transactions", processedCount);
            } else {
                log.info("BOD: No future-dated transactions to process today");
            }

            // Step 3: Check remaining pending future-dated transactions
            long remainingCount = bodValueDateService.getPendingFutureDatedTransactionsCount();
            log.info("BOD: {} future-dated transactions still pending for future dates", remainingCount);
            result.setPendingCountAfter(remainingCount);

            result.setStatus("SUCCESS");
            result.setMessage("BOD processing completed successfully");

            log.info("=".repeat(80));
            log.info("BOD processing completed successfully for date: {}", systemDate);
            log.info("=".repeat(80));

        } catch (Exception e) {
            log.error("=".repeat(80));
            log.error("BOD processing failed for date: {}", systemDate, e);
            log.error("=".repeat(80));

            result.setStatus("FAILED");
            result.setMessage("BOD processing failed: " + e.getMessage());
        }

        return result;
    }

    /**
     * Get status of pending future-dated transactions
     */
    public BODStatus getBODStatus() {
        long pendingCount = bodValueDateService.getPendingFutureDatedTransactionsCount();
        LocalDate systemDate = systemDateService.getSystemDate();

        BODStatus status = new BODStatus();
        status.setSystemDate(systemDate);
        status.setPendingFutureDatedCount(pendingCount);
        status.setPendingTransactions(bodValueDateService.getPendingFutureDatedTransactions());

        return status;
    }

    /**
     * BOD processing result
     */
    @lombok.Data
    public static class BODResult {
        private LocalDate systemDate;
        private long pendingCountBefore;
        private int processedCount;
        private long pendingCountAfter;
        private String status;
        private String message;
    }

    /**
     * BOD status information
     */
    @lombok.Data
    public static class BODStatus {
        private LocalDate systemDate;
        private long pendingFutureDatedCount;
        private java.util.List<com.example.moneymarket.entity.TranTable> pendingTransactions;
    }
}
