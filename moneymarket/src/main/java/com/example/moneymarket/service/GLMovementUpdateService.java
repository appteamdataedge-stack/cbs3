package com.example.moneymarket.service;

import com.example.moneymarket.entity.*;
import com.example.moneymarket.exception.BusinessException;
import com.example.moneymarket.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for Batch Job 4: GL Movement Update
 * Processes verified transactions into GL movement records during EOD
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GLMovementUpdateService {

    private final TranTableRepository tranTableRepository;
    private final GLMovementRepository glMovementRepository;
    private final GLSetupRepository glSetupRepository;
    private final CustAcctMasterRepository custAcctMasterRepository;
    private final OFAcctMasterRepository ofAcctMasterRepository;
    private final SystemDateService systemDateService;

    /**
     * Batch Job 4: GL Movement Update
     *
     * Process:
     * 1. Query all records from tran_table where:
     *    - Tran_Date = System_Date
     *    - Tran_Status = 'Verified'
     *    - NOT already in gl_movement (check by Tran_Id)
     *
     * 2. For each transaction record:
     *    - Get Account_No
     *    - Determine if customer account or office account
     *    - Get Sub_Product_Id from appropriate master table
     *    - Get GL_Num (Cum_GL_Num) from sub_prod_master
     *    - Create record in gl_movement
     *
     * @param systemDate The system date for processing
     * @return Number of GL movement records created
     */
    @Transactional
    public int processGLMovements(LocalDate systemDate) {
        LocalDate processDate = systemDate != null ? systemDate : systemDateService.getSystemDate();
        log.info("Starting Batch Job 4: GL Movement Update for date: {}", processDate);

        // Step 1: Query verified transactions for the system date not yet in gl_movement
        List<TranTable> verifiedTransactions = tranTableRepository.findByTranDateAndTranStatus(
                processDate, TranTable.TranStatus.Verified);

        if (verifiedTransactions.isEmpty()) {
            log.info("No verified transactions found for date: {}", processDate);
            return 0;
        }

        log.info("Found {} verified transactions to process", verifiedTransactions.size());

        // Filter out transactions already in gl_movement
        List<TranTable> transactionsToProcess = filterUnprocessedTransactions(verifiedTransactions);

        log.info("{} transactions need GL movement creation", transactionsToProcess.size());

        int recordsCreated = 0;
        List<String> errors = new ArrayList<>();

        // Step 2: Process each transaction
        for (TranTable transaction : transactionsToProcess) {
            try {
                processTransaction(transaction, processDate);
                recordsCreated++;
            } catch (Exception e) {
                log.error("Error processing transaction ID {}: {}",
                        transaction.getTranId(), e.getMessage());
                errors.add(String.format("Transaction %s: %s", transaction.getTranId(), e.getMessage()));
            }
        }

        log.info("Batch Job 4 completed. Records created: {}, Errors: {}",
                recordsCreated, errors.size());

        if (!errors.isEmpty()) {
            log.warn("GL movement process completed with {} errors: {}",
                    errors.size(), String.join("; ", errors));
        }

        return recordsCreated;
    }

    /**
     * Filter out transactions that already have GL movement records
     */
    private List<TranTable> filterUnprocessedTransactions(List<TranTable> transactions) {
        List<TranTable> unprocessed = new ArrayList<>();

        for (TranTable transaction : transactions) {
            // Check if GL movement already exists for this transaction
            boolean exists = glMovementRepository.existsByTransactionTranId(transaction.getTranId());
            if (!exists) {
                unprocessed.add(transaction);
            } else {
                log.debug("Transaction {} already has GL movement, skipping", transaction.getTranId());
            }
        }

        return unprocessed;
    }

    /**
     * Process a single transaction into GL movement
     */
    private void processTransaction(TranTable transaction, LocalDate systemDate) {
        String accountNo = transaction.getAccountNo();

        if (accountNo == null || accountNo.trim().isEmpty()) {
            throw new BusinessException("Account Number is missing for transaction: " + transaction.getTranId());
        }

        // Determine account type and get GL Number
        String glNum = getGLNumberForAccount(accountNo);

        // Look up GL_Setup record
        GLSetup glSetup = glSetupRepository.findById(glNum)
                .orElseThrow(() -> new BusinessException(
                        String.format("GL Setup not found for GL Number: %s (Account: %s)",
                                glNum, accountNo)));

        // Calculate balance after transaction (will be updated by Batch Job 5)
        BigDecimal balanceAfter = BigDecimal.ZERO; // Placeholder - actual balance calculated in Batch Job 5

        // Create GL_Movement record
        GLMovement glMovement = GLMovement.builder()
                .transaction(transaction)
                .glSetup(glSetup)
                .drCrFlag(transaction.getDrCrFlag())
                .tranDate(systemDate)
                .valueDate(transaction.getValueDate())
                .amount(transaction.getLcyAmt())
                .balanceAfter(balanceAfter)
                .tranCcy(transaction.getTranCcy())
                .fcyAmt(transaction.getFcyAmt())
                .lcyAmt(transaction.getLcyAmt())
                .narration(transaction.getNarration())
                .build();

        glMovementRepository.save(glMovement);

        log.debug("Created GL movement for Transaction {} -> GL {}, Amount: {}",
                transaction.getTranId(), glNum, transaction.getLcyAmt());
    }

    /**
     * Get GL Number for an account
     * Determines if account is customer or office account, then retrieves GL number
     */
    private String getGLNumberForAccount(String accountNo) {
        // First, try to find in customer account master
        Optional<CustAcctMaster> custAcctOpt = custAcctMasterRepository.findById(accountNo);
        if (custAcctOpt.isPresent()) {
            CustAcctMaster custAcct = custAcctOpt.get();
            SubProdMaster subProduct = custAcct.getSubProduct();
            String glNum = subProduct.getCumGLNum();

            if (glNum == null || glNum.trim().isEmpty()) {
                throw new BusinessException(
                        String.format("Cum_GL_Num not configured for sub-product %s (Account: %s)",
                                subProduct.getSubProductCode(), accountNo));
            }

            log.debug("Found customer account {} with GL Number: {}", accountNo, glNum);
            return glNum;
        }

        // If not found in customer accounts, try office accounts
        Optional<OFAcctMaster> ofAcctOpt = ofAcctMasterRepository.findById(accountNo);
        if (ofAcctOpt.isPresent()) {
            OFAcctMaster ofAcct = ofAcctOpt.get();
            SubProdMaster subProduct = ofAcct.getSubProduct();
            String glNum = subProduct.getCumGLNum();

            if (glNum == null || glNum.trim().isEmpty()) {
                throw new BusinessException(
                        String.format("Cum_GL_Num not configured for sub-product %s (Office Account: %s)",
                                subProduct.getSubProductCode(), accountNo));
            }

            log.debug("Found office account {} with GL Number: {}", accountNo, glNum);
            return glNum;
        }

        // Account not found in either master table
        throw new BusinessException(
                String.format("Account %s not found in customer or office account master", accountNo));
    }
}
