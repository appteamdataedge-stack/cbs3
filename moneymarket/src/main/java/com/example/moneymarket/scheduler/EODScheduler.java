package com.example.moneymarket.scheduler;

import com.example.moneymarket.service.EODService;
import com.example.moneymarket.service.InterestAccrualService;
import com.example.moneymarket.service.SystemDateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Scheduled job for End-of-Day processing
 * 
 * Runs automatically at scheduled times to perform:
 * - Interest accrual
 * - Balance aggregation
 * - Double-entry validation
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "eod.scheduler.enabled", havingValue = "true", matchIfMissing = false)
public class EODScheduler {

    private final EODService eodService;
    private final InterestAccrualService interestAccrualService;
    private final SystemDateService systemDateService;

    /**
     * Run EOD processing automatically
     * Scheduled to run at 11:30 PM daily
     */
    @Scheduled(cron = "${eod.scheduler.cron:0 30 23 * * ?}")
    public void runScheduledEOD() {
        log.info("Starting scheduled EOD processing...");
        
        try {
            // Replaced device-based date/time with System_Date (SystemDateService) - CBS Compliance Fix
            LocalDate eodDate = systemDateService.getSystemDate();
            
            // Step 1: Run interest accrual
            log.info("Running interest accrual...");
            int accountsProcessed = interestAccrualService.runEODAccruals(eodDate);
            log.info("Interest accrual completed: {} accounts processed", accountsProcessed);
            
            // Step 2: Run EOD balance processing
            log.info("Running EOD balance processing...");
            EODService.EODSummary summary = eodService.runEODProcessing(eodDate);
            log.info("EOD processing completed: Status={}, Accounts={}, GLs={}, Balanced={}", 
                    summary.getStatus(), summary.getAccountsProcessed(), 
                    summary.getGlsProcessed(), summary.isBalanced());
            
            if (!"SUCCESS".equals(summary.getStatus())) {
                log.error("EOD processing failed: {}", summary.getErrorMessage());
                // In production, send alerts/notifications
            }
            
        } catch (Exception e) {
            log.error("Scheduled EOD processing failed", e);
            // In production, send alerts/notifications
        }
    }

    /**
     * Run interest accrual only
     * Scheduled to run at 11:00 PM daily
     */
    @Scheduled(cron = "${interest.scheduler.cron:0 0 23 * * ?}")
    public void runScheduledInterestAccrual() {
        log.info("Starting scheduled interest accrual...");
        
        try {
            // Replaced device-based date/time with System_Date (SystemDateService) - CBS Compliance Fix
            LocalDate accrualDate = systemDateService.getSystemDate();
            int accountsProcessed = interestAccrualService.runEODAccruals(accrualDate);
            log.info("Interest accrual completed: {} accounts processed", accountsProcessed);
        } catch (Exception e) {
            log.error("Scheduled interest accrual failed", e);
            // In production, send alerts/notifications
        }
    }

    /**
     * Validate system balance periodically
     * Scheduled to run every hour during business hours
     */
    @Scheduled(cron = "${balance.validation.cron:0 0 9-17 * * MON-FRI}")
    public void validateSystemBalance() {
        log.debug("Running balance validation check...");
        
        try {
            // Replaced device-based date/time with System_Date (SystemDateService) - CBS Compliance Fix
            LocalDate today = systemDateService.getSystemDate();
            boolean isBalanced = eodService.validateDoubleEntry(today);
            
            if (!isBalanced) {
                log.warn("System is NOT balanced for date: {}", today);
                // In production, send immediate alerts
            } else {
                log.debug("System is balanced for date: {}", today);
            }
        } catch (Exception e) {
            log.error("Balance validation failed", e);
        }
    }
}
