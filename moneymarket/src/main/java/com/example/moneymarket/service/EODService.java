package com.example.moneymarket.service;

import com.example.moneymarket.entity.*;
import com.example.moneymarket.entity.TranTable.DrCrFlag;
import com.example.moneymarket.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for End-of-Day (EOD) Processing
 * 
 * Responsibilities:
 * - Aggregate GL_movement entries into GL_Balance
 * - Aggregate Tran_Table entries into Acct_Bal
 * - Compute Closing Balance: Closing = Opening + Credits – Debits
 * - Roll forward balances as next day's Opening
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EODService {

    private final TranTableRepository tranTableRepository;
    private final GLMovementRepository glMovementRepository;
    private final AcctBalRepository acctBalRepository;
    private final GLBalanceRepository glBalanceRepository;
    private final CustAcctMasterRepository custAcctMasterRepository;
    private final GLSetupRepository glSetupRepository;
    private final SystemDateService systemDateService;

    /**
     * Run complete End-of-Day processing
     * 
     * @param eodDate The EOD date (defaults to current date if null)
     * @return Summary of EOD processing
     */
    @Transactional
    public EODSummary runEODProcessing(LocalDate eodDate) {
        // Replaced device-based date/time with System_Date (SystemDateService) - CBS Compliance Fix
        if (eodDate == null) {
            eodDate = systemDateService.getSystemDate();
        }
        
        log.info("Starting EOD processing for date: {}", eodDate);
        
        EODSummary summary = new EODSummary();
        summary.setEodDate(eodDate);
        summary.setStartTime(systemDateService.getSystemDateTime());
        
        try {
            // Step 1: Process Account Balances
            int accountsProcessed = processAccountBalances(eodDate);
            summary.setAccountsProcessed(accountsProcessed);
            log.info("Processed {} account balances", accountsProcessed);
            
            // Step 2: Process GL Balances
            int glsProcessed = processGLBalances(eodDate);
            summary.setGlsProcessed(glsProcessed);
            log.info("Processed {} GL balances", glsProcessed);
            
            // Step 3: Validate double-entry consistency
            boolean isBalanced = validateDoubleEntry(eodDate);
            summary.setBalanced(isBalanced);
            
            if (!isBalanced) {
                log.warn("EOD validation failed: System is not balanced!");
                summary.setStatus("FAILED");
                summary.setErrorMessage("System is not balanced");
            } else {
                summary.setStatus("SUCCESS");
                log.info("EOD processing completed successfully");
            }
            
        } catch (Exception e) {
            log.error("EOD processing failed", e);
            summary.setStatus("FAILED");
            summary.setErrorMessage(e.getMessage());
            throw e;
        } finally {
            // Replaced device-based date/time with System_Date (SystemDateService) - CBS Compliance Fix
            summary.setEndTime(systemDateService.getSystemDateTime());
        }
        
        return summary;
    }

    /**
     * Process Account Balances for EOD
     * Aggregates Tran_Table entries and updates Acct_Bal
     * 
     * @param eodDate The EOD date
     * @return Number of accounts processed
     */
    @Transactional
    public int processAccountBalances(LocalDate eodDate) {
        log.info("Processing account balances for date: {}", eodDate);
        
        // Get all accounts
        List<CustAcctMaster> accounts = custAcctMasterRepository.findAll();
        int processed = 0;
        
        for (CustAcctMaster account : accounts) {
            String accountNo = account.getAccountNo();
            
            // Get account balance
            AcctBal balance = acctBalRepository.findLatestByAccountNo(accountNo)
                    .orElse(AcctBal.builder()
                            .accountNo(accountNo)
                            .currentBalance(BigDecimal.ZERO)
                            .availableBalance(BigDecimal.ZERO)
                            // Replaced device-based date/time with System_Date (SystemDateService) - CBS Compliance Fix
                            .lastUpdated(systemDateService.getSystemDateTime())
                            .build());
            
            // Get today's transactions
            BigDecimal todayDebits = tranTableRepository.sumDebitTransactionsForAccountOnDate(accountNo, eodDate)
                    .orElse(BigDecimal.ZERO);
            
            BigDecimal todayCredits = tranTableRepository.sumCreditTransactionsForAccountOnDate(accountNo, eodDate)
                    .orElse(BigDecimal.ZERO);
            
            // Calculate closing balance: Opening + Credits - Debits
            // Note: For asset accounts, debits increase balance
            // For liability accounts, credits increase balance
            BigDecimal openingBalance = balance.getCurrentBalance();
            BigDecimal closingBalance = openingBalance.add(todayCredits).subtract(todayDebits);
            
            // Update balance
            balance.setCurrentBalance(closingBalance);
            balance.setAvailableBalance(closingBalance); // Simplified: available = current
            // Replaced device-based date/time with System_Date (SystemDateService) - CBS Compliance Fix
            balance.setLastUpdated(systemDateService.getSystemDateTime());
            
            acctBalRepository.save(balance);
            processed++;
            
            log.debug("Account {}: Opening={}, Debits={}, Credits={}, Closing={}", 
                    accountNo, openingBalance, todayDebits, todayCredits, closingBalance);
        }
        
        return processed;
    }

    /**
     * Process GL Balances for EOD
     * Aggregates GL_movement entries and updates GL_Balance
     * 
     * @param eodDate The EOD date
     * @return Number of GLs processed
     */
    @Transactional
    public int processGLBalances(LocalDate eodDate) {
        log.info("Processing GL balances for date: {}", eodDate);
        
        // Get all GLs
        List<GLSetup> glSetups = glSetupRepository.findAll();
        int processed = 0;
        
        for (GLSetup glSetup : glSetups) {
            String glNum = glSetup.getGlNum();
            
            // Get GL balance
            GLBalance balance = glBalanceRepository.findLatestByGlNum(glNum)
                    .orElse(GLBalance.builder()
                            .glNum(glNum)
                            .glSetup(glSetup)
                            .currentBalance(BigDecimal.ZERO)
                            // Replaced device-based date/time with System_Date (SystemDateService) - CBS Compliance Fix
                            .lastUpdated(systemDateService.getSystemDateTime())
                            .build());
            
            // Get today's GL movements
            List<GLMovement> todayMovements = glMovementRepository.findByGlSetupAndTranDate(glSetup, eodDate);
            
            BigDecimal todayDebits = todayMovements.stream()
                    .filter(m -> m.getDrCrFlag() == DrCrFlag.D)
                    .map(GLMovement::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal todayCredits = todayMovements.stream()
                    .filter(m -> m.getDrCrFlag() == DrCrFlag.C)
                    .map(GLMovement::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Calculate closing balance based on GL type
            BigDecimal openingBalance = balance.getCurrentBalance();
            BigDecimal closingBalance;
            
            // Asset and Expense GLs: Debit increases, Credit decreases
            // Liability, Equity, and Income GLs: Credit increases, Debit decreases
            if (isDebitNatureGL(glSetup)) {
                closingBalance = openingBalance.add(todayDebits).subtract(todayCredits);
            } else {
                closingBalance = openingBalance.add(todayCredits).subtract(todayDebits);
            }
            
            // Update balance
            balance.setCurrentBalance(closingBalance);
            // Replaced device-based date/time with System_Date (SystemDateService) - CBS Compliance Fix
            balance.setLastUpdated(systemDateService.getSystemDateTime());
            
            glBalanceRepository.save(balance);
            processed++;
            
            log.debug("GL {}: Opening={}, Debits={}, Credits={}, Closing={}", 
                    glNum, openingBalance, todayDebits, todayCredits, closingBalance);
        }
        
        return processed;
    }

    /**
     * Validate double-entry consistency
     * Ensures total debits equal total credits for the day
     * 
     * @param eodDate The EOD date
     * @return true if balanced, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean validateDoubleEntry(LocalDate eodDate) {
        log.info("Validating double-entry for date: {}", eodDate);
        
        // Get all transactions for the day
        List<TranTable> todayTransactions = tranTableRepository.findByTranDateBetween(eodDate, eodDate);
        
        BigDecimal totalDebits = todayTransactions.stream()
                .filter(t -> t.getDrCrFlag() == DrCrFlag.D)
                .map(TranTable::getLcyAmt)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalCredits = todayTransactions.stream()
                .filter(t -> t.getDrCrFlag() == DrCrFlag.C)
                .map(TranTable::getLcyAmt)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        boolean isBalanced = totalDebits.compareTo(totalCredits) == 0;
        
        log.info("Double-entry validation: Debits={}, Credits={}, Balanced={}", 
                totalDebits, totalCredits, isBalanced);
        
        return isBalanced;
    }

    /**
     * Determine if a GL is debit nature (Asset or Expense)
     * 
     * @param glSetup The GL setup
     * @return true if debit nature, false otherwise
     */
    private boolean isDebitNatureGL(GLSetup glSetup) {
        // Layer 1: 1=Assets, 2=Liabilities, 3=Equity, 4=Income, 5=Expenses
        String glNum = glSetup.getGlNum();
        if (glNum == null || glNum.isEmpty()) {
            return false;
        }
        
        char firstDigit = glNum.charAt(0);
        return firstDigit == '1' || firstDigit == '5'; // Assets or Expenses
    }

    /**
     * EOD Summary DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class EODSummary {
        private LocalDate eodDate;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private int accountsProcessed;
        private int glsProcessed;
        private boolean balanced;
        private String status;
        private String errorMessage;
    }
}

