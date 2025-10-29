package com.example.moneymarket.controller;

import com.example.moneymarket.service.EODService;
import com.example.moneymarket.service.InterestAccrualService;
import com.example.moneymarket.service.SystemDateService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * REST controller for End-of-Day (EOD) operations
 */
@RestController
@RequestMapping("/api/eod")
@RequiredArgsConstructor
public class EODController {

    private final EODService eodService;
    private final InterestAccrualService interestAccrualService;
    private final SystemDateService systemDateService;

    /**
     * Run EOD processing for a specific date
     * 
     * @param date The EOD date (optional, defaults to current date)
     * @return EOD processing summary
     */
    @PostMapping("/process")
    public ResponseEntity<EODService.EODSummary> runEOD(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        EODService.EODSummary summary = eodService.runEODProcessing(date);
        return ResponseEntity.ok(summary);
    }

    /**
     * Run interest accrual processing
     * 
     * @param date The accrual date (optional, defaults to current date)
     * @return Number of accounts processed
     */
    @PostMapping("/interest-accrual")
    public ResponseEntity<InterestAccrualResponse> runInterestAccrual(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        int accountsProcessed = interestAccrualService.runEODAccruals(date);
        return ResponseEntity.ok(new InterestAccrualResponse(accountsProcessed, "Success"));
    }

    /**
     * Validate double-entry for a specific date
     * 
     * @param date The date to validate (optional, defaults to current date)
     * @return Validation result
     */
    @GetMapping("/validate")
    public ResponseEntity<ValidationResponse> validateDoubleEntry(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) {
            // Replaced device-based date/time with System_Date (SystemDateService) - CBS Compliance Fix
            date = systemDateService.getSystemDate();
        }
        boolean isBalanced = eodService.validateDoubleEntry(date);
        return ResponseEntity.ok(new ValidationResponse(isBalanced, 
                isBalanced ? "System is balanced" : "System is NOT balanced"));
    }

    /**
     * Process account balances only
     * 
     * @param date The date to process (optional, defaults to current date)
     * @return Number of accounts processed
     */
    @PostMapping("/process-accounts")
    public ResponseEntity<ProcessResponse> processAccountBalances(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) {
            // Replaced device-based date/time with System_Date (SystemDateService) - CBS Compliance Fix
            date = systemDateService.getSystemDate();
        }
        int processed = eodService.processAccountBalances(date);
        return ResponseEntity.ok(new ProcessResponse(processed, "Account balances processed successfully"));
    }

    /**
     * Process GL balances only
     * 
     * @param date The date to process (optional, defaults to current date)
     * @return Number of GLs processed
     */
    @PostMapping("/process-gls")
    public ResponseEntity<ProcessResponse> processGLBalances(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) {
            // Replaced device-based date/time with System_Date (SystemDateService) - CBS Compliance Fix
            date = systemDateService.getSystemDate();
        }
        int processed = eodService.processGLBalances(date);
        return ResponseEntity.ok(new ProcessResponse(processed, "GL balances processed successfully"));
    }

    // Response DTOs
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class InterestAccrualResponse {
        private int accountsProcessed;
        private String message;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ValidationResponse {
        private boolean balanced;
        private String message;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ProcessResponse {
        private int itemsProcessed;
        private String message;
    }
}

