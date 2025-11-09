package com.example.moneymarket.service;

import com.example.moneymarket.entity.TranTable;
import com.example.moneymarket.entity.TranTable.TranStatus;
import com.example.moneymarket.entity.GLMovement;
import com.example.moneymarket.entity.GLSetup;
import com.example.moneymarket.exception.ResourceNotFoundException;
import com.example.moneymarket.repository.TranTableRepository;
import com.example.moneymarket.repository.GLMovementRepository;
import com.example.moneymarket.repository.GLSetupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for processing future-dated transactions at BOD (Beginning of Day)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BODValueDateService {

    private final TranTableRepository tranTableRepository;
    private final GLMovementRepository glMovementRepository;
    private final GLSetupRepository glSetupRepository;
    private final BalanceService balanceService;
    private final UnifiedAccountService unifiedAccountService;
    private final ValueDatePostingService valueDatePostingService;
    private final TransactionValidationService validationService;
    private final SystemDateService systemDateService;

    /**
     * Process all future-dated transactions whose value date has arrived
     * Called during BOD processing
     *
     * @return Number of transactions processed
     */
    @Transactional
    public int processFutureDatedTransactions() {
        LocalDate systemDate = systemDateService.getSystemDate();

        // Find all future-dated transactions whose value date = today or earlier
        List<TranTable> futureTransactions = tranTableRepository.findAll().stream()
            .filter(t -> t.getTranStatus() == TranStatus.Future &&
                        !t.getValueDate().isAfter(systemDate))
            .toList();

        if (futureTransactions.isEmpty()) {
            log.info("BOD: No future-dated transactions to process for date {}", systemDate);
            return 0;
        }

        log.info("BOD: Processing {} future-dated transactions for date {}",
            futureTransactions.size(), systemDate);

        int processedCount = 0;

        for (TranTable transaction : futureTransactions) {
            try {
                processFutureDatedTransaction(transaction, systemDate);
                processedCount++;
            } catch (Exception e) {
                log.error("BOD: Error processing future-dated transaction {}: {}",
                    transaction.getTranId(), e.getMessage(), e);
            }
        }

        log.info("BOD: Successfully processed {} future-dated transactions", processedCount);
        return processedCount;
    }

    /**
     * Process a single future-dated transaction
     * Updates status to Posted, updates balances, creates GL movements
     */
    private void processFutureDatedTransaction(TranTable transaction, LocalDate systemDate) {
        log.debug("BOD: Processing future-dated transaction: {}", transaction.getTranId());

        // Update status from Future to Posted
        transaction.setTranStatus(TranStatus.Posted);

        // Update transaction date to current system date (value date remains original)
        transaction.setTranDate(systemDate);

        // Get GL number from account
        String glNum = unifiedAccountService.getGlNum(transaction.getAccountNo());
        GLSetup glSetup = glSetupRepository.findById(glNum)
            .orElseThrow(() -> new ResourceNotFoundException("GL", "GL Number", glNum));

        // Validate transaction before posting
        try {
            validationService.validateTransaction(
                transaction.getAccountNo(), transaction.getDrCrFlag(), transaction.getLcyAmt());
        } catch (Exception e) {
            log.error("BOD: Validation failed for future-dated transaction {}: {}",
                transaction.getTranId(), e.getMessage());
            throw e;
        }

        // Update account balance
        validationService.updateAccountBalanceForTransaction(
            transaction.getAccountNo(), transaction.getDrCrFlag(), transaction.getLcyAmt());

        // Update GL balance
        BigDecimal newGLBalance = balanceService.updateGLBalance(
            glNum, transaction.getDrCrFlag(), transaction.getLcyAmt());

        // Create GL movement record
        GLMovement glMovement = GLMovement.builder()
            .transaction(transaction)
            .glSetup(glSetup)
            .drCrFlag(transaction.getDrCrFlag())
            .tranDate(systemDate)  // Current system date
            .valueDate(transaction.getValueDate())  // Original value date
            .amount(transaction.getLcyAmt())
            .balanceAfter(newGLBalance)
            .build();

        // Save transaction and GL movement
        tranTableRepository.save(transaction);
        glMovementRepository.save(glMovement);

        // Update value date log to mark as posted
        valueDatePostingService.updateAdjustmentPostedFlag(transaction.getTranId(), "Y");

        log.debug("BOD: Future-dated transaction {} posted successfully. " +
            "Status: {}, Tran Date: {}, Value Date: {}",
            transaction.getTranId(), transaction.getTranStatus(),
            transaction.getTranDate(), transaction.getValueDate());
    }

    /**
     * Get count of pending future-dated transactions
     *
     * @return Number of future-dated transactions not yet posted
     */
    public long getPendingFutureDatedTransactionsCount() {
        return tranTableRepository.findAll().stream()
            .filter(t -> t.getTranStatus() == TranStatus.Future)
            .count();
    }

    /**
     * Get all pending future-dated transactions
     *
     * @return List of future-dated transactions
     */
    public List<TranTable> getPendingFutureDatedTransactions() {
        return tranTableRepository.findAll().stream()
            .filter(t -> t.getTranStatus() == TranStatus.Future)
            .toList();
    }
}
