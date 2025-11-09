package com.example.moneymarket.controller;

import com.example.moneymarket.scheduler.BODScheduler;
import com.example.moneymarket.scheduler.BODScheduler.BODResult;
import com.example.moneymarket.scheduler.BODScheduler.BODStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for BOD (Beginning of Day) operations
 * Handles manual BOD processing for value-dated transactions
 */
@RestController
@RequestMapping("/api/bod")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "BOD", description = "Beginning of Day (BOD) operations for value dating")
public class BODController {

    private final BODScheduler bodScheduler;

    /**
     * Run BOD processing manually
     * Processes all future-dated transactions whose value date has arrived
     *
     * @return BOD processing result
     */
    @PostMapping("/run")
    @Operation(summary = "Run BOD processing", description = "Manually trigger BOD processing to post future-dated transactions")
    public ResponseEntity<BODResult> runBOD() {
        log.info("Received request to run manual BOD processing");

        try {
            BODResult result = bodScheduler.runManualBOD();
            log.info("BOD processing completed: Status={}, Processed={} transactions",
                result.getStatus(), result.getProcessedCount());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error running BOD processing", e);

            BODResult errorResult = new BODResult();
            errorResult.setStatus("ERROR");
            errorResult.setMessage("BOD processing failed: " + e.getMessage());

            return ResponseEntity.internalServerError().body(errorResult);
        }
    }

    /**
     * Get BOD status information
     * Returns pending future-dated transactions count and details
     *
     * @return BOD status
     */
    @GetMapping("/status")
    @Operation(summary = "Get BOD status", description = "Get information about pending future-dated transactions")
    public ResponseEntity<BODStatus> getBODStatus() {
        log.info("Received request for BOD status");

        try {
            BODStatus status = bodScheduler.getBODStatus();
            log.info("BOD status retrieved: {} pending transactions", status.getPendingFutureDatedCount());

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            log.error("Error getting BOD status", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
