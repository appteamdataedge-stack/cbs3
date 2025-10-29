package com.example.moneymarket.service;

import com.example.moneymarket.entity.EODLogTable;
import com.example.moneymarket.entity.GLMovementAccrual;
import com.example.moneymarket.entity.GLSetup;
import com.example.moneymarket.entity.InttAccrTran;
import com.example.moneymarket.exception.BusinessException;
import com.example.moneymarket.repository.EODLogTableRepository;
import com.example.moneymarket.repository.GLMovementAccrualRepository;
import com.example.moneymarket.repository.GLSetupRepository;
import com.example.moneymarket.repository.InttAccrTranRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for Batch Job 3: Interest Accrual GL Movement Update
 * Processes interest accrual transactions into GL movement accrual records
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InterestAccrualGLMovementService {

    private final InttAccrTranRepository inttAccrTranRepository;
    private final GLMovementAccrualRepository glMovementAccrualRepository;
    private final GLSetupRepository glSetupRepository;
    private final EODLogTableRepository eodLogTableRepository;
    private final SystemDateService systemDateService;

    /**
     * Batch Job 3: Interest Accrual GL Movement Update
     *
     * Process:
     * 1. Query records from intt_accr_tran where Accrual_Date = System_Date AND Status = 'Pending'
     * 2. For each record:
     *    - Check if GL movement already exists (duplicate prevention)
     *    - Extract GL_Account_No (this is the GL number)
     *    - Look up GL_Num from gl_setup where GL_Num = first 9 digits of GL_Account_No
     *    - Create record in gl_movement_accrual
     *    - Update intt_accr_tran status to 'Posted'
     *
     * @param systemDate The system date for processing
     * @return Number of GL movement accrual records created
     */
    @Transactional
    public int processInterestAccrualGLMovements(LocalDate systemDate) {
        LocalDate processDate = systemDate != null ? systemDate : systemDateService.getSystemDate();
        log.info("Starting Batch Job 3: Interest Accrual GL Movement Update for date: {}", processDate);

        // Step 1: Query PENDING interest accrual transactions for the system date
        List<InttAccrTran> accrualTransactions = inttAccrTranRepository
                .findByAccrualDateAndStatus(processDate, InttAccrTran.AccrualStatus.Pending);

        if (accrualTransactions.isEmpty()) {
            log.info("No pending interest accrual transactions found for date: {}", processDate);
            return 0;
        }

        log.info("Found {} pending interest accrual transactions to process", accrualTransactions.size());

        int recordsCreated = 0;
        List<String> errors = new ArrayList<>();

        // Step 2: Process each accrual transaction
        for (InttAccrTran accrualTran : accrualTransactions) {
            try {
                processAccrualTransaction(accrualTran, processDate);
                recordsCreated++;
            } catch (Exception e) {
                log.error("Error processing accrual transaction ID {}: {}",
                        accrualTran.getAccrTranId(), e.getMessage());
                errors.add(String.format("Accrual ID %s: %s", accrualTran.getAccrTranId(), e.getMessage()));
            }
        }

        log.info("Batch Job 3 completed. Records created: {}, Errors: {}",
                recordsCreated, errors.size());

        if (!errors.isEmpty()) {
            log.warn("GL movement accrual process completed with {} errors: {}",
                    errors.size(), String.join("; ", errors));
        }

        return recordsCreated;
    }

    /**
     * Process a single accrual transaction into GL movement accrual
     * Includes duplicate prevention and status update
     */
    private void processAccrualTransaction(InttAccrTran accrualTran, LocalDate systemDate) {
        String accrTranId = accrualTran.getAccrTranId();

        // DUPLICATE PREVENTION: Check if GL movement already exists for this Accr_Tran_Id
        if (glMovementAccrualRepository.existsByAccrualAccrTranId(accrTranId)) {
            log.warn("Skipping duplicate GL movement for Accr_Tran_Id: {} (already processed)", accrTranId);
            return;  // Skip this record
        }

        // Extract GL_Account_No from the accrual transaction
        String glAccountNo = accrualTran.getGlAccountNo();

        if (glAccountNo == null || glAccountNo.trim().isEmpty()) {
            throw new BusinessException("GL Account Number is missing for accrual ID: " + accrTranId);
        }

        // Extract first 9 digits of GL_Account_No to get GL_Num
        String glNum = extractGLNum(glAccountNo);

        // Look up GL_Setup record
        GLSetup glSetup = glSetupRepository.findById(glNum)
                .orElseThrow(() -> new BusinessException(
                        String.format("GL Setup not found for GL Number: %s (from GL Account: %s)",
                                glNum, glAccountNo)));

        // Create GL_Movement_Accrual record
        GLMovementAccrual glMovementAccrual = GLMovementAccrual.builder()
                .accrual(accrualTran)
                .glSetup(glSetup)
                .drCrFlag(accrualTran.getDrCrFlag())
                .accrualDate(systemDate)
                .tranDate(accrualTran.getTranDate())
                .tranId(null)  // Set to null for interest accruals
                .amount(accrualTran.getAmount())
                .tranCcy(accrualTran.getTranCcy())
                .fcyAmt(accrualTran.getFcyAmt())
                .exchangeRate(accrualTran.getExchangeRate())
                .lcyAmt(accrualTran.getLcyAmt())
                .narration(accrualTran.getNarration())
                .status(InttAccrTran.AccrualStatus.Pending)  // GL movement starts as Pending
                .build();

        glMovementAccrualRepository.save(glMovementAccrual);

        // STATUS UPDATE: Mark the intt_accr_tran record as Posted to prevent reprocessing
        accrualTran.setStatus(InttAccrTran.AccrualStatus.Posted);
        inttAccrTranRepository.save(accrualTran);

        log.info("Created GL movement accrual for Accr_Tran_Id {} -> GL {}, Amount: {}, Status updated to Posted",
                accrTranId, glNum, accrualTran.getAmount());
    }

    /**
     * Extract GL Number (first 9 digits) from GL Account Number
     *
     * @param glAccountNo The full GL account number
     * @return The first 9 digits representing the GL Number
     */
    private String extractGLNum(String glAccountNo) {
        if (glAccountNo == null) {
            throw new BusinessException("GL Account Number cannot be null");
        }

        // Remove any whitespace
        String trimmedGLAccountNo = glAccountNo.trim();

        // If the GL account number is already 9 or fewer digits, return as is
        if (trimmedGLAccountNo.length() <= 9) {
            return trimmedGLAccountNo;
        }

        // Extract first 9 digits
        return trimmedGLAccountNo.substring(0, 9);
    }

}
