package com.example.moneymarket.controller;

import com.example.moneymarket.service.EODJobManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for individual EOD job execution management
 */
@RestController
@RequestMapping("/api/admin/eod/jobs")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:5173")
public class EODJobController {

    private final EODJobManagementService eodJobManagementService;

    /**
     * Execute a specific EOD job
     *
     * @param jobNumber The job number (1-8)
     * @param userId The user executing the job (optional, defaults to "SYSTEM")
     * @return Job execution result
     */
    @PostMapping("/execute/{jobNumber}")
    public ResponseEntity<EODJobManagementService.EODJobResult> executeJob(
            @PathVariable int jobNumber,
            @RequestParam(defaultValue = "SYSTEM") String userId) {
        
        log.info("Received request to execute EOD Job {} by user: {}", jobNumber, userId);
        
        try {
            EODJobManagementService.EODJobResult result = eodJobManagementService.executeJob(jobNumber, userId);
            
            if (result.isSuccess()) {
                log.info("Job {} executed successfully: {}", jobNumber, result.getMessage());
                return ResponseEntity.ok(result);
            } else {
                log.warn("Job {} execution failed: {}", jobNumber, result.getMessage());
                return ResponseEntity.badRequest().body(result);
            }
            
        } catch (Exception e) {
            log.error("Error executing job {}: {}", jobNumber, e.getMessage(), e);
            EODJobManagementService.EODJobResult errorResult = EODJobManagementService.EODJobResult
                    .failure(jobNumber, "Job " + jobNumber, "Internal server error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }

    /**
     * Get the status of all EOD jobs for today
     *
     * @return List of job statuses
     */
    @GetMapping("/status")
    public ResponseEntity<List<EODJobManagementService.EODJobStatus>> getJobStatuses() {
        log.info("Received request to get EOD job statuses");
        
        try {
            List<EODJobManagementService.EODJobStatus> statuses = eodJobManagementService.getJobStatuses();
            log.info("Retrieved {} job statuses", statuses.size());
            return ResponseEntity.ok(statuses);
            
        } catch (Exception e) {
            log.error("Error retrieving job statuses: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Check if a specific job can be executed
     *
     * @param jobNumber The job number to check
     * @return True if the job can be executed, false otherwise
     */
    @GetMapping("/can-execute/{jobNumber}")
    public ResponseEntity<Boolean> canExecuteJob(@PathVariable int jobNumber) {
        log.info("Received request to check if job {} can be executed", jobNumber);
        
        try {
            boolean canExecute = eodJobManagementService.canExecuteJob(jobNumber);
            log.info("Job {} can be executed: {}", jobNumber, canExecute);
            return ResponseEntity.ok(canExecute);
            
        } catch (Exception e) {
            log.error("Error checking if job {} can be executed: {}", jobNumber, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(false);
        }
    }

    /**
     * Get detailed information about a specific job
     *
     * @param jobNumber The job number
     * @return Job status information
     */
    @GetMapping("/{jobNumber}")
    public ResponseEntity<EODJobManagementService.EODJobStatus> getJobStatus(@PathVariable int jobNumber) {
        log.info("Received request to get status for job {}", jobNumber);
        
        try {
            List<EODJobManagementService.EODJobStatus> allStatuses = eodJobManagementService.getJobStatuses();
            
            EODJobManagementService.EODJobStatus jobStatus = allStatuses.stream()
                    .filter(status -> status.getJobNumber() == jobNumber)
                    .findFirst()
                    .orElse(null);
            
            if (jobStatus != null) {
                log.info("Retrieved status for job {}: {}", jobNumber, jobStatus.getStatus());
                return ResponseEntity.ok(jobStatus);
            } else {
                log.warn("Job {} not found", jobNumber);
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            log.error("Error retrieving status for job {}: {}", jobNumber, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
