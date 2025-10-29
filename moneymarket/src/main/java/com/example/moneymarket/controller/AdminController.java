package com.example.moneymarket.controller;

import com.example.moneymarket.service.InterestAccrualService;
import com.example.moneymarket.service.AccountBalanceUpdateService;
import com.example.moneymarket.service.SystemDateService;
import com.example.moneymarket.service.EODOrchestrationService;
import com.example.moneymarket.service.EODValidationService;
import com.example.moneymarket.service.FinancialReportsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for administrative operations
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final InterestAccrualService interestAccrualService;
    private final AccountBalanceUpdateService accountBalanceUpdateService;
    private final SystemDateService systemDateService;
    private final EODOrchestrationService eodOrchestrationService;
    private final EODValidationService eodValidationService;
    private final com.example.moneymarket.service.InterestAccrualGLMovementService interestAccrualGLMovementService;
    private final com.example.moneymarket.service.GLMovementUpdateService glMovementUpdateService;
    private final com.example.moneymarket.service.GLBalanceUpdateService glBalanceUpdateService;
    private final com.example.moneymarket.service.InterestAccrualAccountBalanceService interestAccrualAccountBalanceService;
    private final FinancialReportsService financialReportsService;
    private final com.example.moneymarket.repository.GLMovementRepository glMovementRepository;
    private final com.example.moneymarket.repository.GLMovementAccrualRepository glMovementAccrualRepository;

    @Value("${reports.directory:reports}")
    private String reportsBaseDirectory;

    /**
     * Run End of Day (EOD) process manually
     * 
     * @param date Optional date to run EOD for (defaults to current date)
     * @param userId The user ID running the EOD (defaults to ADMIN)
     * @return The response with EOD execution results
     */
    @PostMapping("/run-eod")
    public ResponseEntity<Map<String, Object>> runEOD(
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "ADMIN") String userId) {
        
        try {
            EODOrchestrationService.EODResult result = eodOrchestrationService.executeEOD(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            response.put("accountsProcessed", result.getAccountsProcessed());
            response.put("interestEntriesProcessed", result.getInterestEntriesProcessed());
            response.put("glMovementsProcessed", result.getGlMovementsProcessed());
            response.put("glMovementsUpdated", result.getGlMovementsUpdated());
            response.put("glBalancesUpdated", result.getGlBalancesUpdated());
            response.put("accrualBalancesUpdated", result.getAccrualBalancesUpdated());
            response.put("timestamp", java.time.LocalDateTime.now());
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.internalServerError().body(response);
            }
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "EOD execution failed: " + e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Execute Account Balance Update batch job
     * 
     * @param request The request containing the system date
     * @return The response with number of processed accounts
     */
    @PostMapping("/eod/account-balance-update")
    public ResponseEntity<Map<String, Object>> executeAccountBalanceUpdate(
            @RequestBody Map<String, String> request) {
        
        String systemDateStr = request.get("systemDate");
        if (systemDateStr == null || systemDateStr.isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "System date is required");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        try {
            LocalDate systemDate = LocalDate.parse(systemDateStr);
            
            // Set the system date for the application
            systemDateService.setSystemDate(systemDate, "ADMIN");
            
            int accountsProcessed = accountBalanceUpdateService.executeAccountBalanceUpdate(systemDate);
            
            // Validate the update
            boolean isValid = accountBalanceUpdateService.validateAccountBalanceUpdate(systemDate);
            
            Map<String, Object> response = new HashMap<>();
            response.put("systemDate", systemDate);
            response.put("accountsProcessed", accountsProcessed);
            response.put("status", "Completed");
            response.put("validated", isValid);
            response.put("message", String.format("Account Balance Update completed successfully. Processed %d accounts.", accountsProcessed));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to execute Account Balance Update: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Get current system date and EOD status
     */
    @GetMapping("/eod/status")
    public ResponseEntity<Map<String, Object>> getEODStatus() {
        try {
            LocalDate systemDate = eodOrchestrationService.getSystemDate();
            
            Map<String, Object> response = new HashMap<>();
            response.put("systemDate", systemDate);
            response.put("currentDate", LocalDate.now());
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get EOD status: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Run pre-EOD validations only
     */
    @PostMapping("/eod/validate")
    public ResponseEntity<Map<String, Object>> validateEOD(
            @RequestParam(defaultValue = "ADMIN") String userId) {
        
        try {
            LocalDate systemDate = eodOrchestrationService.getSystemDate();
            EODValidationService.EODValidationResult result = eodValidationService.performPreEODValidations(userId, systemDate);
            
            Map<String, Object> response = new HashMap<>();
            response.put("valid", result.isValid());
            response.put("message", result.getMessage());
            response.put("systemDate", systemDate);
            response.put("timestamp", java.time.LocalDateTime.now());
            
            if (result.isValid()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("valid", false);
            errorResponse.put("message", "Validation failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Set the system date
     *
     * @param systemDateStr The system date to set (YYYY-MM-DD format)
     * @return The response with updated system date
     */
    @PostMapping("/set-system-date")
    public ResponseEntity<Map<String, Object>> setSystemDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String systemDateStr) {

        try {
            LocalDate systemDate = LocalDate.parse(systemDateStr);

            // Update the system date in Parameter_Table
            systemDateService.setSystemDate(systemDate, "ADMIN");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "System date successfully updated");
            response.put("systemDate", systemDate);
            response.put("timestamp", java.time.LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to update system date: " + e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    // ========== Individual Batch Job Endpoints ==========

    /**
     * Batch Job 2: Interest Accrual Transaction Update
     */
    @PostMapping("/eod/batch/interest-accrual")
    public ResponseEntity<Map<String, Object>> executeInterestAccrual() {
        try {
            LocalDate systemDate = eodOrchestrationService.getSystemDate();
            int recordsProcessed = interestAccrualService.runEODAccruals(systemDate);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("jobName", "Interest Accrual Transaction Update");
            response.put("recordsProcessed", recordsProcessed);
            response.put("message", "Batch Job 2 completed successfully");
            response.put("systemDate", systemDate);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("Batch Job 2 failed: " + e.getMessage());
        }
    }

    /**
     * Batch Job 3: Interest Accrual GL Movement Update
     */
    @PostMapping("/eod/batch/interest-accrual-gl")
    public ResponseEntity<Map<String, Object>> executeInterestAccrualGL() {
        try {
            LocalDate systemDate = eodOrchestrationService.getSystemDate();
            int recordsProcessed = interestAccrualGLMovementService.processInterestAccrualGLMovements(systemDate);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("jobName", "Interest Accrual GL Movement Update");
            response.put("recordsProcessed", recordsProcessed);
            response.put("message", "Batch Job 3 completed successfully");
            response.put("systemDate", systemDate);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("Batch Job 3 failed: " + e.getMessage());
        }
    }

    /**
     * Batch Job 4: GL Movement Update
     */
    @PostMapping("/eod/batch/gl-movement")
    public ResponseEntity<Map<String, Object>> executeGLMovement() {
        try {
            LocalDate systemDate = eodOrchestrationService.getSystemDate();
            int recordsProcessed = glMovementUpdateService.processGLMovements(systemDate);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("jobName", "GL Movement Update");
            response.put("recordsProcessed", recordsProcessed);
            response.put("message", "Batch Job 4 completed successfully");
            response.put("systemDate", systemDate);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("Batch Job 4 failed: " + e.getMessage());
        }
    }

    /**
     * Batch Job 5: GL Balance Update
     */
    @PostMapping("/eod/batch/gl-balance")
    public ResponseEntity<Map<String, Object>> executeGLBalance() {
        try {
            LocalDate systemDate = eodOrchestrationService.getSystemDate();
            int recordsProcessed = glBalanceUpdateService.updateGLBalances(systemDate);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("jobName", "GL Balance Update");
            response.put("recordsProcessed", recordsProcessed);
            response.put("message", "Batch Job 5 completed successfully - Books are balanced!");
            response.put("systemDate", systemDate);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("Batch Job 5 failed: " + e.getMessage());
        }
    }

    /**
     * Batch Job 6: Interest Accrual Account Balance Update
     */
    @PostMapping("/eod/batch/interest-accrual-balance")
    public ResponseEntity<Map<String, Object>> executeInterestAccrualBalance() {
        try {
            LocalDate systemDate = eodOrchestrationService.getSystemDate();
            int recordsProcessed = interestAccrualAccountBalanceService.updateInterestAccrualAccountBalances(systemDate);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("jobName", "Interest Accrual Account Balance Update");
            response.put("recordsProcessed", recordsProcessed);
            response.put("message", "Batch Job 6 completed successfully");
            response.put("systemDate", systemDate);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("Batch Job 6 failed: " + e.getMessage());
        }
    }

    /**
     * Batch Job 7: Financial Reports Generation
     */
    @PostMapping("/eod/batch-job-7/execute")
    public ResponseEntity<Map<String, Object>> executeBatchJob7(@RequestParam(required = false) String date) {
        try {
            LocalDate reportDate;
            if (date != null && !date.isEmpty()) {
                reportDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyyMMdd"));
            } else {
                reportDate = eodOrchestrationService.getSystemDate();
            }
            
            Map<String, String> result = financialReportsService.generateFinancialReports(reportDate);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Reports generated successfully");
            response.put("reportDate", result.get("reportDate"));
            response.put("trialBalanceFileName", "TrialBalance_" + result.get("reportDate") + ".csv");
            response.put("balanceSheetFileName", "BalanceSheet_" + result.get("reportDate") + ".xlsx");  // Excel format

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("Batch Job 7 failed: " + e.getMessage());
        }
    }

    /**
     * Download Trial Balance CSV file
     */
    @GetMapping("/eod/batch-job-7/download/trial-balance/{date}")
    public ResponseEntity<byte[]> downloadTrialBalance(@PathVariable String date) {
        try {
            validateDateFormat(date);
            
            String fileName = "TrialBalance_" + date + ".csv";
            Path filePath = Paths.get(reportsBaseDirectory, date, fileName);
            
            byte[] content = financialReportsService.readCsvFileAsBytes(filePath.toString());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setContentLength(content.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
                    
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Download Balance Sheet Excel file (.xlsx)
     */
    @GetMapping("/eod/batch-job-7/download/balance-sheet/{date}")
    public ResponseEntity<byte[]> downloadBalanceSheet(@PathVariable String date) {
        try {
            validateDateFormat(date);
            
            String fileName = "BalanceSheet_" + date + ".xlsx";  // Excel format
            Path filePath = Paths.get(reportsBaseDirectory, date, fileName);
            
            byte[] content = financialReportsService.readCsvFileAsBytes(filePath.toString());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));  // Excel MIME type
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setContentLength(content.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
                    
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Validate date format (YYYYMMDD)
     */
    private void validateDateFormat(String date) {
        if (date == null || date.isEmpty()) {
            throw new IllegalArgumentException("Date cannot be empty");
        }
        if (date.length() != 8) {
            throw new IllegalArgumentException("Date must be 8 digits (YYYYMMDD)");
        }
        if (!date.matches("\\d{8}")) {
            throw new IllegalArgumentException("Date must contain only digits");
        }
        if (date.contains("..") || date.contains("/") || date.contains("\\")) {
            throw new IllegalArgumentException("Invalid date format");
        }
        try {
            LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyyMMdd"));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date");
        }
    }

    /**
     * Helper method to create error response
     */
    private ResponseEntity<Map<String, Object>> createErrorResponse(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", message);
        errorResponse.put("timestamp", java.time.LocalDateTime.now());
        return ResponseEntity.internalServerError().body(errorResponse);
    }

    /**
     * TEST ENDPOINT: Get distinct GL numbers for a given date
     * This endpoint is for debugging Batch Job 5 - it tests the native queries directly
     */
    @GetMapping("/test/gl-numbers/{date}")
    public ResponseEntity<Map<String, Object>> testGLNumbers(@PathVariable String date) {
        try {
            LocalDate testDate = LocalDate.parse(date);
            
            // Test the repository methods directly
            java.util.List<String> movementGLNums = glMovementRepository.findDistinctGLNumbersByTranDate(testDate);
            java.util.List<String> accrualGLNums = glMovementAccrualRepository.findDistinctGLNumbersByAccrualDate(testDate);
            
            // Combine unique GL numbers
            java.util.Set<String> allGLNums = new java.util.HashSet<>();
            allGLNums.addAll(movementGLNums);
            allGLNums.addAll(accrualGLNums);
            
            Map<String, Object> result = new HashMap<>();
            result.put("date", testDate);
            result.put("glNumbersFromMovement", movementGLNums);
            result.put("glNumbersFromAccrual", accrualGLNums);
            result.put("allUniqueGLNumbers", allGLNums);
            result.put("totalCount", allGLNums.size());
            result.put("success", true);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("stackTrace", e.getClass().getName());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
