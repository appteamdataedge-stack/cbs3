package com.example.moneymarket.service;

import com.example.moneymarket.entity.EODLogTable;
import com.example.moneymarket.repository.EODLogTableRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing individual EOD job execution with sequential enforcement
 */
@Service
@Slf4j
public class EODJobManagementService {

    private final EODLogTableRepository eodLogTableRepository;
    private final SystemDateService systemDateService;
    private final AccountBalanceUpdateService accountBalanceUpdateService;
    private final InterestAccrualService interestAccrualService;
    private final InterestAccrualGLMovementService interestAccrualGLMovementService;
    private final GLMovementUpdateService glMovementUpdateService;
    private final GLBalanceUpdateService glBalanceUpdateService;
    private final InterestAccrualAccountBalanceService interestAccrualAccountBalanceService;
    private final FinancialReportsService financialReportsService;
    private final EODOrchestrationService eodOrchestrationService;

    // Self-reference for Spring AOP proxy to enable @Transactional on methods called within same class
    // @Lazy breaks the circular dependency
    private final EODJobManagementService self;

    public EODJobManagementService(
            EODLogTableRepository eodLogTableRepository,
            SystemDateService systemDateService,
            AccountBalanceUpdateService accountBalanceUpdateService,
            InterestAccrualService interestAccrualService,
            InterestAccrualGLMovementService interestAccrualGLMovementService,
            GLMovementUpdateService glMovementUpdateService,
            GLBalanceUpdateService glBalanceUpdateService,
            InterestAccrualAccountBalanceService interestAccrualAccountBalanceService,
            FinancialReportsService financialReportsService,
            EODOrchestrationService eodOrchestrationService,
            @Lazy EODJobManagementService self) {
        this.eodLogTableRepository = eodLogTableRepository;
        this.systemDateService = systemDateService;
        this.accountBalanceUpdateService = accountBalanceUpdateService;
        this.interestAccrualService = interestAccrualService;
        this.interestAccrualGLMovementService = interestAccrualGLMovementService;
        this.glMovementUpdateService = glMovementUpdateService;
        this.glBalanceUpdateService = glBalanceUpdateService;
        this.interestAccrualAccountBalanceService = interestAccrualAccountBalanceService;
        this.financialReportsService = financialReportsService;
        this.eodOrchestrationService = eodOrchestrationService;
        this.self = self;
    }

    /**
     * Execute a specific EOD job with proper logging and sequential enforcement
     *
     * @param jobNumber The job number (1-8)
     * @param userId The user executing the job
     * @return Job execution result
     */
    public EODJobResult executeJob(int jobNumber, String userId) {
        LocalDate systemDate = systemDateService.getSystemDate();
        String jobName = getJobName(jobNumber);

        log.info("Executing EOD Job {}: {} for user: {}", jobNumber, jobName, userId);

        // Check if job has already been executed successfully today
        Optional<EODLogTable> existingExecution = eodLogTableRepository
                .findTopByEodDateAndJobNameOrderByStartTimestampDesc(systemDate, jobName);

        if (existingExecution.isPresent() && existingExecution.get().getStatus() == EODLogTable.EODStatus.Success) {
            log.warn("Job {} ({}) has already been executed successfully today at {}",
                    jobNumber, jobName, existingExecution.get().getStartTimestamp());
            return EODJobResult.alreadyExecuted(jobNumber, jobName, existingExecution.get().getStartTimestamp());
        }

        // Check if previous job is required and completed
        if (jobNumber > 1) {
            EODJobResult previousJobCheck = checkPreviousJobCompletion(jobNumber - 1, systemDate);
            if (!previousJobCheck.isSuccess()) {
                return EODJobResult.previousJobNotCompleted(jobNumber, jobName, previousJobCheck.getMessage());
            }
        }

        // Log job start in a separate transaction (use self to invoke proxy)
        EODLogTable logEntry = self.logJobStartInNewTransaction(systemDate, jobName, userId);

        try {
            // Execute the specific job
            int recordsProcessed = executeSpecificJob(jobNumber, systemDate);

            // Log job success in a separate transaction (use self to invoke proxy)
            self.logJobSuccessInNewTransaction(logEntry, recordsProcessed);

            log.info("Job {} ({}) completed successfully. Records processed: {}",
                    jobNumber, jobName, recordsProcessed);

            // Check if all jobs are completed after Job 8 finishes
            if (jobNumber == 8) {
                checkAndCompleteEODCycle(systemDate, userId);
            }

            return EODJobResult.success(jobNumber, jobName, recordsProcessed);

        } catch (Exception e) {
            // Log job failure in a separate transaction (use self to invoke proxy)
            self.logJobFailureInNewTransaction(logEntry, e.getMessage(), "Job execution");

            log.error("Job {} ({}) failed: {}", jobNumber, jobName, e.getMessage(), e);

            return EODJobResult.failure(jobNumber, jobName, e.getMessage());
        }
    }

    /**
     * Get the status of all EOD jobs for today
     */
    @Transactional(readOnly = true)
    public List<EODJobStatus> getJobStatuses() {
        LocalDate systemDate = systemDateService.getSystemDate();
        
        return List.of(
                getJobStatus(1, "Account Balance Update", systemDate),
                getJobStatus(2, "Interest Accrual Transaction Update", systemDate),
                getJobStatus(3, "Interest Accrual GL Movement Update", systemDate),
                getJobStatus(4, "GL Movement Update", systemDate),
                getJobStatus(5, "GL Balance Update", systemDate),
                getJobStatus(6, "Interest Accrual Account Balance Update", systemDate),
                getJobStatus(7, "Financial Reports Generation", systemDate),
                getJobStatus(8, "System Date Increment", systemDate)
        );
    }

    /**
     * Check if a specific job can be executed (previous job completed)
     */
    @Transactional(readOnly = true)
    public boolean canExecuteJob(int jobNumber) {
        LocalDate systemDate = systemDateService.getSystemDate();
        
        if (jobNumber == 1) {
            return true; // First job can always be executed
        }
        
        EODJobResult previousJobCheck = checkPreviousJobCompletion(jobNumber - 1, systemDate);
        return previousJobCheck.isSuccess();
    }

    private EODJobResult checkPreviousJobCompletion(int previousJobNumber, LocalDate systemDate) {
        String previousJobName = getJobName(previousJobNumber);
        
        Optional<EODLogTable> previousExecution = eodLogTableRepository
                .findTopByEodDateAndJobNameOrderByStartTimestampDesc(systemDate, previousJobName);
        
        if (previousExecution.isEmpty()) {
            return EODJobResult.failure(previousJobNumber, previousJobName, 
                    "Previous job has not been executed yet");
        }
        
        if (previousExecution.get().getStatus() != EODLogTable.EODStatus.Success) {
            return EODJobResult.failure(previousJobNumber, previousJobName, 
                    "Previous job did not complete successfully");
        }
        
        return EODJobResult.success(previousJobNumber, previousJobName, 0);
    }

    private EODJobStatus getJobStatus(int jobNumber, String jobName, LocalDate systemDate) {
        Optional<EODLogTable> execution = eodLogTableRepository
                .findTopByEodDateAndJobNameOrderByStartTimestampDesc(systemDate, jobName);
        
        // Determine if this job can be executed
        boolean canExecute = canExecuteJob(jobNumber);
        
        if (execution.isEmpty()) {
            return new EODJobStatus(jobNumber, jobName, "pending", null, 0, null, canExecute);
        }
        
        EODLogTable logEntry = execution.get();
        switch (logEntry.getStatus()) {
            case Success:
                // If system date has advanced, jobs become available again
                if (logEntry.getEodDate().isBefore(systemDate)) {
                    return new EODJobStatus(jobNumber, jobName, "pending", null, 0, null, canExecute);
                }
                return new EODJobStatus(jobNumber, jobName, "completed", logEntry.getStartTimestamp(), 
                        logEntry.getRecordsProcessed(), null, false);
            case Failed:
                // If system date has advanced, failed jobs become available again
                if (logEntry.getEodDate().isBefore(systemDate)) {
                    return new EODJobStatus(jobNumber, jobName, "pending", null, 0, null, canExecute);
                }
                return new EODJobStatus(jobNumber, jobName, "failed", logEntry.getStartTimestamp(), 
                        0, logEntry.getErrorMessage(), canExecute);
            case Running:
                return new EODJobStatus(jobNumber, jobName, "running", logEntry.getStartTimestamp(), 
                        0, null, false);
            default:
                return new EODJobStatus(jobNumber, jobName, "pending", null, 0, null, canExecute);
        }
    }

    private int executeSpecificJob(int jobNumber, LocalDate systemDate) {
        return switch (jobNumber) {
            case 1 -> accountBalanceUpdateService.executeAccountBalanceUpdate(systemDate);
            case 2 -> interestAccrualService.runEODAccruals(systemDate);
            case 3 -> interestAccrualGLMovementService.processInterestAccrualGLMovements(systemDate);
            case 4 -> glMovementUpdateService.processGLMovements(systemDate);
            case 5 -> glBalanceUpdateService.updateGLBalances(systemDate);
            case 6 -> interestAccrualAccountBalanceService.updateInterestAccrualAccountBalances(systemDate);
            case 7 -> {
                var reports = financialReportsService.generateFinancialReports(systemDate);
                // Return 2 since we generate 2 reports (Trial Balance and Balance Sheet)
                yield 2;
            }
            case 8 -> {
                // System Date Increment - call actual implementation
                log.info("Executing Batch Job 8 via Job Management Service");
                boolean success = eodOrchestrationService.executeBatchJob8(systemDate, systemDate, "SYSTEM");
                log.info("Batch Job 8 completed via Job Management Service: {}", success);
                yield success ? 1 : 0;
            }
            default -> throw new IllegalArgumentException("Invalid job number: " + jobNumber);
        };
    }

    private String getJobName(int jobNumber) {
        return switch (jobNumber) {
            case 1 -> "Account Balance Update";
            case 2 -> "Interest Accrual Transaction Update";
            case 3 -> "Interest Accrual GL Movement Update";
            case 4 -> "GL Movement Update";
            case 5 -> "GL Balance Update";
            case 6 -> "Interest Accrual Account Balance Update";
            case 7 -> "Financial Reports Generation";
            case 8 -> "System Date Increment";
            default -> throw new IllegalArgumentException("Invalid job number: " + jobNumber);
        };
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EODLogTable logJobStartInNewTransaction(LocalDate eodDate, String jobName, String userId) {
        EODLogTable logEntry = EODLogTable.builder()
                .eodDate(eodDate)
                .jobName(jobName)
                .startTimestamp(systemDateService.getSystemDateTime())
                .systemDate(eodDate)
                .userId(userId)
                .status(EODLogTable.EODStatus.Running)
                .createdTimestamp(systemDateService.getSystemDateTime())
                .build();

        return eodLogTableRepository.save(logEntry);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logJobSuccessInNewTransaction(EODLogTable logEntry, int recordsProcessed) {
        // Fetch the entity again in this new transaction to avoid detached entity issues
        EODLogTable managedEntry = eodLogTableRepository.findById(logEntry.getEodLogId())
                .orElseThrow(() -> new RuntimeException("Log entry not found"));

        managedEntry.setEndTimestamp(systemDateService.getSystemDateTime());
        managedEntry.setRecordsProcessed(recordsProcessed);
        managedEntry.setStatus(EODLogTable.EODStatus.Success);
        eodLogTableRepository.save(managedEntry);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logJobFailureInNewTransaction(EODLogTable logEntry, String errorMessage, String failedAtStep) {
        // Fetch the entity again in this new transaction to avoid detached entity issues
        EODLogTable managedEntry = eodLogTableRepository.findById(logEntry.getEodLogId())
                .orElseThrow(() -> new RuntimeException("Log entry not found"));

        managedEntry.setEndTimestamp(systemDateService.getSystemDateTime());
        managedEntry.setStatus(EODLogTable.EODStatus.Failed);
        managedEntry.setErrorMessage(errorMessage);
        managedEntry.setFailedAtStep(failedAtStep);
        eodLogTableRepository.save(managedEntry);
    }

    /**
     * Check if all EOD jobs are completed and automatically increment system date if so
     */
    @Transactional
    private void checkAndCompleteEODCycle(LocalDate currentSystemDate, String userId) {
        log.info("Checking if all EOD jobs are completed for system date: {}", currentSystemDate);
        
        // Check if jobs 1-7 are completed successfully for the current system date
        // Job 8 (System Date Increment) is the trigger, so we check if jobs 1-7 are done
        boolean allJobsCompleted = true;
        for (int jobNumber = 1; jobNumber <= 7; jobNumber++) {
            String jobName = getJobName(jobNumber);
            Optional<EODLogTable> execution = eodLogTableRepository
                    .findTopByEodDateAndJobNameOrderByStartTimestampDesc(currentSystemDate, jobName);
            
            if (execution.isEmpty() || execution.get().getStatus() != EODLogTable.EODStatus.Success) {
                allJobsCompleted = false;
                log.debug("Job {} ({}) is not completed yet", jobNumber, jobName);
                break;
            }
        }
        
        if (allJobsCompleted) {
            log.info("Jobs 1-7 completed successfully. Job 8 (System Date Increment) completed. Automatically incrementing system date and resetting job cycle.");
            
            // Increment system date
            LocalDate newSystemDate = currentSystemDate.plusDays(1);
            updateSystemDate(newSystemDate, userId);
            
            // Log the EOD cycle completion
            logEODJob(currentSystemDate, "EOD Cycle Complete", currentSystemDate, userId, 8,
                     EODLogTable.EODStatus.Success, null, "All jobs completed, system date incremented to " + newSystemDate);
            
            log.info("EOD cycle completed successfully. System date incremented from {} to {}", 
                    currentSystemDate, newSystemDate);
        } else {
            log.info("Jobs 1-7 are not all completed yet. EOD cycle will continue when all jobs are done.");
        }
    }

    /**
     * Update system date in Parameter_Table
     */
    private void updateSystemDate(LocalDate newSystemDate, String userId) {
        log.info("Updating system date from {} to {}", systemDateService.getSystemDate(), newSystemDate);
        
        // Use SystemDateService to update the system date
        try {
            systemDateService.setSystemDate(newSystemDate, userId);
            log.info("System date successfully updated to: {}", newSystemDate);
        } catch (Exception e) {
            log.error("Failed to update system date: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update system date: " + e.getMessage(), e);
        }
    }

    /**
     * Log EOD job execution
     */
    private void logEODJob(LocalDate eodDate, String jobName, LocalDate systemDate, String userId, 
                         int recordsProcessed, EODLogTable.EODStatus status, String errorMessage, String details) {
        EODLogTable logEntry = EODLogTable.builder()
                .eodDate(eodDate)
                .jobName(jobName)
                .startTimestamp(systemDateService.getSystemDateTime())
                .endTimestamp(systemDateService.getSystemDateTime())
                .systemDate(systemDate)
                .userId(userId)
                .status(status)
                .recordsProcessed(recordsProcessed)
                .errorMessage(errorMessage)
                .failedAtStep(details)
                .createdTimestamp(systemDateService.getSystemDateTime())
                .build();

        eodLogTableRepository.save(logEntry);
    }

    /**
     * EOD Job Result class
     */
    public static class EODJobResult {
        private final int jobNumber;
        private final String jobName;
        private final boolean success;
        private final String message;
        private final int recordsProcessed;
        private final LocalDateTime executionTime;

        private EODJobResult(int jobNumber, String jobName, boolean success, String message, 
                           int recordsProcessed, LocalDateTime executionTime) {
            this.jobNumber = jobNumber;
            this.jobName = jobName;
            this.success = success;
            this.message = message;
            this.recordsProcessed = recordsProcessed;
            this.executionTime = executionTime;
        }

        public static EODJobResult success(int jobNumber, String jobName, int recordsProcessed) {
            return new EODJobResult(jobNumber, jobName, true, "Job completed successfully", 
                    recordsProcessed, null);
        }

        public static EODJobResult failure(int jobNumber, String jobName, String errorMessage) {
            return new EODJobResult(jobNumber, jobName, false, errorMessage, 0, null);
        }

        public static EODJobResult alreadyExecuted(int jobNumber, String jobName, LocalDateTime executionTime) {
            return new EODJobResult(jobNumber, jobName, true, "Job already executed successfully", 
                    0, executionTime);
        }

        public static EODJobResult previousJobNotCompleted(int jobNumber, String jobName, String reason) {
            return new EODJobResult(jobNumber, jobName, false, 
                    "Cannot execute job: " + reason, 0, null);
        }

        // Getters
        public int getJobNumber() { return jobNumber; }
        public String getJobName() { return jobName; }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public int getRecordsProcessed() { return recordsProcessed; }
        public LocalDateTime getExecutionTime() { return executionTime; }
    }

    /**
     * EOD Job Status class
     */
    public static class EODJobStatus {
        private final int jobNumber;
        private final String jobName;
        private final String status; // "pending", "running", "completed", "failed"
        private final LocalDateTime executionTime;
        private final int recordsProcessed;
        private final String errorMessage;
        private final boolean canExecute;

        private EODJobStatus(int jobNumber, String jobName, String status, LocalDateTime executionTime,
                           int recordsProcessed, String errorMessage, boolean canExecute) {
            this.jobNumber = jobNumber;
            this.jobName = jobName;
            this.status = status;
            this.executionTime = executionTime;
            this.recordsProcessed = recordsProcessed;
            this.errorMessage = errorMessage;
            this.canExecute = canExecute;
        }

        public static EODJobStatus pending(int jobNumber, String jobName) {
            return new EODJobStatus(jobNumber, jobName, "pending", null, 0, null, true);
        }

        public static EODJobStatus running(int jobNumber, String jobName, LocalDateTime executionTime) {
            return new EODJobStatus(jobNumber, jobName, "running", executionTime, 0, null, false);
        }

        public static EODJobStatus completed(int jobNumber, String jobName, LocalDateTime executionTime, int recordsProcessed) {
            return new EODJobStatus(jobNumber, jobName, "completed", executionTime, recordsProcessed, null, false);
        }

        public static EODJobStatus failed(int jobNumber, String jobName, LocalDateTime executionTime, String errorMessage) {
            return new EODJobStatus(jobNumber, jobName, "failed", executionTime, 0, errorMessage, true);
        }

        // Getters
        public int getJobNumber() { return jobNumber; }
        public String getJobName() { return jobName; }
        public String getStatus() { return status; }
        public LocalDateTime getExecutionTime() { return executionTime; }
        public int getRecordsProcessed() { return recordsProcessed; }
        public String getErrorMessage() { return errorMessage; }
        public boolean isCanExecute() { return canExecute; }
    }
}
