package com.example.moneymarket.service;

import com.example.moneymarket.entity.AcctBal;
import com.example.moneymarket.entity.TranTable;
import com.example.moneymarket.repository.AcctBalRepository;
import com.example.moneymarket.repository.TranTableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for Account Balance Update batch job (EOD Batch Job 1)
 * 
 * Logic as per specification:
 * FOR EACH unique Account_No in Tran_Table where Tran_Date = System_Date:
 * 1. Get previous day's Closing_Bal as Opening_Bal (if Opening_Bal is not null then do not need to use below logics)
 * 2. If no previous record, Opening_Bal = 0
 * 3. Calculate DR_Summation: Sum of LCY_Amt where Dr_Cr_Flag = 'D' and Tran_Status = 'Verified'
 * 4. Calculate CR_Summation: Sum of LCY_Amt where Dr_Cr_Flag = 'C' and Tran_Status = 'Verified'
 * 5. Calculate Closing_Bal: Closing_Bal = Opening_Bal + CR_Summation - DR_Summation
 * 6. Insert/Update record in Acct_Bal with composite primary key (Tran_Date, Account_No)
 * 
 * Validation: Verify all accounts with transactions have been updated
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountBalanceUpdateService {

    private final AcctBalRepository acctBalRepository;
    private final TranTableRepository tranTableRepository;
    private final SystemDateService systemDateService;

    /**
     * Execute Account Balance Update batch job (EOD Batch Job 1)
     * 
     * @param systemDate The system date to process
     * @return Number of accounts processed
     */
    @Transactional
    public int executeAccountBalanceUpdate(LocalDate systemDate) {
        log.info("Starting EOD Account Balance Update for date: {}", systemDate);
        
        // Get all unique account numbers that have transactions on the system date
        List<String> accountsWithTransactions = tranTableRepository.findByTranDateBetween(systemDate, systemDate)
                .stream()
                .map(TranTable::getAccountNo)
                .distinct()
                .collect(Collectors.toList());
        
        if (accountsWithTransactions.isEmpty()) {
            log.info("No accounts with transactions found for EOD processing on date: {}", systemDate);
            return 0;
        }
        
        int processedCount = 0;
        int errorCount = 0;
        
        for (String accountNo : accountsWithTransactions) {
            try {
                processAccountBalance(accountNo, systemDate);
                processedCount++;
                log.debug("Processed EOD balance for account: {} on date: {}", accountNo, systemDate);
            } catch (Exception e) {
                errorCount++;
                log.error("Error processing EOD balance for account: {} on date: {}", accountNo, systemDate, e);
                // Continue processing other accounts instead of failing the entire batch
                // Don't re-throw the exception to avoid marking the transaction as rollback-only
            }
        }
        
        log.info("EOD Account Balance Update completed. Processed: {}, Errors: {}, Total Accounts with Transactions: {} for date: {}", 
                processedCount, errorCount, accountsWithTransactions.size(), systemDate);
        
        if (errorCount > 0) {
            log.warn("EOD processing completed with {} errors out of {} accounts", errorCount, accountsWithTransactions.size());
        }
        
        // If there were errors but we still processed some accounts, that's acceptable
        // Only fail the entire transaction if no accounts were processed at all
        if (processedCount == 0 && errorCount > 0) {
            throw new RuntimeException("Failed to process any accounts during EOD balance update");
        }
        
        return processedCount;
    }
    
    /**
     * Process account balance for a specific account and date
     * Implements the EOD logic as per specification
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void processAccountBalance(String accountNo, LocalDate systemDate) {
        // Check if account balance record already exists for this date
        Optional<AcctBal> existingRecord = acctBalRepository.findByAccountNoAndTranDate(accountNo, systemDate);
        
        BigDecimal openingBal;
        
        if (existingRecord.isPresent() && existingRecord.get().getOpeningBal() != null) {
            // If Opening_Bal is not null then do not need to use below logics
            openingBal = existingRecord.get().getOpeningBal();
            log.debug("Using existing Opening_Bal for account: {} on date: {}, value: {}", 
                    accountNo, systemDate, openingBal);
        } else {
            // Get previous day's Closing_Bal as Opening_Bal
            openingBal = getPreviousDayClosingBalance(accountNo, systemDate);
            log.debug("Calculated Opening_Bal for account: {} on date: {}, value: {}", 
                    accountNo, systemDate, openingBal);
        }
        
        // Calculate DR_Summation and CR_Summation for Verified transactions only
        Map<String, BigDecimal> summations = calculateDebitCreditSummations(accountNo, systemDate);
        BigDecimal drSummation = summations.get("DR");
        BigDecimal crSummation = summations.get("CR");
        
        // Handle NULL sums
        if (drSummation == null) drSummation = BigDecimal.ZERO;
        if (crSummation == null) crSummation = BigDecimal.ZERO;
        
        // Calculate Closing_Bal: Closing_Bal = Opening_Bal + CR_Summation - DR_Summation
        BigDecimal closingBal = openingBal.add(crSummation).subtract(drSummation);
        
        // Insert/Update record in Acct_Bal (INSERT ... ON DUPLICATE KEY UPDATE pattern)
        AcctBal accountBalance;
        if (existingRecord.isPresent()) {
            // Update existing record
            accountBalance = existingRecord.get();
            accountBalance.setOpeningBal(openingBal);
            accountBalance.setDrSummation(drSummation);
            accountBalance.setCrSummation(crSummation);
            accountBalance.setClosingBal(closingBal);
            accountBalance.setCurrentBalance(closingBal);
            accountBalance.setAvailableBalance(closingBal);
            // Replaced device-based date/time with System_Date (SystemDateService) - CBS Compliance Fix
            accountBalance.setLastUpdated(systemDateService.getSystemDateTime());
            log.debug("Updating existing EOD record for account: {} on date: {}", accountNo, systemDate);
        } else {
            // Insert new record
            accountBalance = AcctBal.builder()
                    .tranDate(systemDate)
                    .accountNo(accountNo)
                    .openingBal(openingBal)
                    .drSummation(drSummation)
                    .crSummation(crSummation)
                    .closingBal(closingBal)
                    .currentBalance(closingBal)
                    .availableBalance(closingBal)
                    // Replaced device-based date/time with System_Date (SystemDateService) - CBS Compliance Fix
                    .lastUpdated(systemDateService.getSystemDateTime())
                    .build();
            log.debug("Creating new EOD record for account: {} on date: {}", accountNo, systemDate);
        }
        
        acctBalRepository.save(accountBalance);
        log.debug("EOD balance processed for account: {} on date: {} - Opening: {}, DR: {}, CR: {}, Closing: {}", 
                accountNo, systemDate, openingBal, drSummation, crSummation, closingBal);
    }
    
    /**
     * Get previous day's closing balance for an account using 3-tier fallback logic
     *
     * This method implements the standardized 3-tier fallback logic for opening balance retrieval:
     * - Tier 1: Previous day's record (systemDate - 1)
     * - Tier 2: Last transaction date (MAX(Tran_Date) < systemDate)
     * - Tier 3: New account (return 0)
     *
     * This method is PUBLIC and shared across services to ensure consistent logic.
     * Used by: Batch Job 1, Transaction Validation, Balance Service
     *
     * @param accountNo The account number
     * @param systemDate The system date
     * @return Opening balance (previous day's closing balance)
     */
    public BigDecimal getPreviousDayClosingBalance(String accountNo, LocalDate systemDate) {
        // Get all account balance records for this account before the system date
        List<AcctBal> accountBalances = acctBalRepository
                .findByAccountNoAndTranDateBeforeOrderByTranDateDesc(accountNo, systemDate);

        // Tier 3: If no previous record exists at all, Opening_Bal = 0 (new account)
        if (accountBalances.isEmpty()) {
            log.info("3-Tier Fallback [Tier 3 - New Account]: Account {} has no previous records before {}. Using Opening_Bal = 0",
                    accountNo, systemDate);
            return BigDecimal.ZERO;
        }

        // Tier 1: Try to get the previous day's record
        LocalDate previousDay = systemDate.minusDays(1);
        Optional<AcctBal> previousDayRecord = accountBalances.stream()
                .filter(acctBal -> previousDay.equals(acctBal.getTranDate()))
                .findFirst();

        if (previousDayRecord.isPresent()) {
            BigDecimal previousDayClosingBal = previousDayRecord.get().getClosingBal();
            if (previousDayClosingBal == null) {
                previousDayClosingBal = BigDecimal.ZERO;
            }
            log.debug("3-Tier Fallback [Tier 1 - Previous Day]: Account {} found record for {} with Closing_Bal = {}",
                    accountNo, previousDay, previousDayClosingBal);
            return previousDayClosingBal;
        }

        // Tier 2: Previous day's record doesn't exist, use last transaction date
        AcctBal lastRecord = accountBalances.get(0); // First in sorted list (most recent)
        BigDecimal lastClosingBal = lastRecord.getClosingBal();
        if (lastClosingBal == null) {
            lastClosingBal = BigDecimal.ZERO;
        }

        long daysSinceLastRecord = java.time.temporal.ChronoUnit.DAYS.between(lastRecord.getTranDate(), systemDate);
        log.warn("3-Tier Fallback [Tier 2 - Last Transaction]: Account {} has gap of {} days. Previous day {} not found. " +
                "Using last Closing_Bal from {} = {}",
                accountNo, daysSinceLastRecord, previousDay, lastRecord.getTranDate(), lastClosingBal);

        return lastClosingBal;
    }
    
    
    /**
     * Calculate debit and credit summations for an account on a specific date
     * Only includes transactions with Tran_Status = 'Verified' as per specification
     */
    private Map<String, BigDecimal> calculateDebitCreditSummations(String accountNo, LocalDate systemDate) {
        // Get all transactions for the account on the system date with Verified status only
        List<TranTable> transactions = tranTableRepository.findByAccountNoAndTranDate(accountNo, systemDate).stream()
                .filter(t -> t.getTranStatus() == TranTable.TranStatus.Verified) // Only Verified transactions
                .collect(Collectors.toList());
        
        BigDecimal drSummation = BigDecimal.ZERO;
        BigDecimal crSummation = BigDecimal.ZERO;
        
        for (TranTable transaction : transactions) {
            if (transaction.getDrCrFlag() == TranTable.DrCrFlag.D) {
                drSummation = drSummation.add(transaction.getLcyAmt());
            } else if (transaction.getDrCrFlag() == TranTable.DrCrFlag.C) {
                crSummation = crSummation.add(transaction.getLcyAmt());
            }
        }
        
        log.debug("Calculated summations for account: {} on date: {} - DR: {}, CR: {} (from {} verified transactions)", 
                accountNo, systemDate, drSummation, crSummation, transactions.size());
        
        return Map.of(
                "DR", drSummation,
                "CR", crSummation
        );
    }
    
    /**
     * Validate that all accounts with transactions have been processed for EOD
     * 
     * @param systemDate The system date to validate
     * @return true if validation passes, false otherwise
     */
    public boolean validateAccountBalanceUpdate(LocalDate systemDate) {
        // Get all unique account numbers that have transactions on the system date
        List<String> accountsWithTransactions = tranTableRepository.findByTranDateBetween(systemDate, systemDate)
                .stream()
                .map(TranTable::getAccountNo)
                .distinct()
                .collect(Collectors.toList());
        
        // Get all account numbers that have been updated for the system date
        List<String> accountNosWithUpdatedBalances = acctBalRepository.findByTranDate(systemDate)
                .stream()
                .map(AcctBal::getAccountNo)
                .distinct()
                .collect(Collectors.toList());
        
        // Check if all accounts with transactions have been processed
        boolean isValid = accountsWithTransactions.containsAll(accountNosWithUpdatedBalances) &&
                         accountNosWithUpdatedBalances.containsAll(accountsWithTransactions);
        
        if (!isValid) {
            log.warn("EOD validation failed on date: {}. " +
                    "Accounts with transactions: {}, Accounts with EOD records: {}",
                    systemDate, accountsWithTransactions.size(), accountNosWithUpdatedBalances.size());
            
            // Log missing accounts
            List<String> missingAccounts = accountsWithTransactions.stream()
                    .filter(accountNo -> !accountNosWithUpdatedBalances.contains(accountNo))
                    .collect(Collectors.toList());
            
            if (!missingAccounts.isEmpty()) {
                log.warn("Missing EOD records for {} accounts with transactions: {}", missingAccounts.size(), missingAccounts);
            }
        } else {
            log.info("EOD validation passed for date: {} - All {} accounts with transactions processed", 
                    systemDate, accountsWithTransactions.size());
        }
        
        return isValid;
    }
}

