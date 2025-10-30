package com.example.moneymarket.service;

import com.example.moneymarket.entity.AcctBal;
import com.example.moneymarket.entity.AcctBalAccrual;
import com.example.moneymarket.entity.GLBalance;
import com.example.moneymarket.entity.CustAcctMaster;
import com.example.moneymarket.entity.OFAcctMaster;
import com.example.moneymarket.entity.TranTable.DrCrFlag;
import com.example.moneymarket.exception.ResourceNotFoundException;
import com.example.moneymarket.repository.AcctBalRepository;
import com.example.moneymarket.repository.AcctBalAccrualRepository;
import com.example.moneymarket.repository.CustAcctMasterRepository;
import com.example.moneymarket.repository.OFAcctMasterRepository;
import com.example.moneymarket.repository.GLBalanceRepository;
import com.example.moneymarket.repository.TranTableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing account and GL balances
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BalanceService {

    private final AcctBalRepository acctBalRepository;
    private final AcctBalAccrualRepository acctBalAccrualRepository;
    private final CustAcctMasterRepository custAcctMasterRepository;
    private final OFAcctMasterRepository ofAcctMasterRepository;
    private final GLBalanceRepository glBalanceRepository;
    private final TranTableRepository tranTableRepository;
    private final SystemDateService systemDateService;
    private final AccountBalanceUpdateService accountBalanceUpdateService;

    /**
     * Update account balance for a transaction with pessimistic locking to prevent races
     * 
     * @param accountNo The account number
     * @param drCrFlag The debit/credit flag
     * @param amount The transaction amount
     * @return The updated balance
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    @Retryable(retryFor = {Exception.class}, maxAttempts = 3)
    public BigDecimal updateAccountBalance(String accountNo, DrCrFlag drCrFlag, BigDecimal amount) {
        // Get the latest account balance record with lock
        AcctBal balance = acctBalRepository.findLatestByAccountNo(accountNo)
                .orElseThrow(() -> new ResourceNotFoundException("Account Balance", "Account Number", accountNo));

        BigDecimal oldBalance = balance.getCurrentBalance();
        BigDecimal newBalance;

        // Update balance based on debit/credit flag
        if (drCrFlag == DrCrFlag.D) {
            newBalance = oldBalance.add(amount); // Debit increases asset accounts
        } else {
            newBalance = oldBalance.subtract(amount); // Credit decreases asset accounts
        }

        // Update balance
        balance.setCurrentBalance(newBalance);
        balance.setAvailableBalance(newBalance); // In this simple implementation, current = available
        // Replaced device-based date/time with System_Date (SystemDateService) - CBS Compliance Fix
        balance.setLastUpdated(systemDateService.getSystemDateTime());

        // Save updated balance
        acctBalRepository.save(balance);

        log.info("Account balance updated for account {}: {} {} {} = {}", 
                accountNo, oldBalance, drCrFlag, amount, newBalance);

        return newBalance;
    }

    /**
     * Update GL balance for a transaction with pessimistic locking to prevent races
     * 
     * @param glNum The GL number
     * @param drCrFlag The debit/credit flag
     * @param amount The transaction amount
     * @return The updated balance
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    @Retryable(retryFor = {Exception.class}, maxAttempts = 3)
    public BigDecimal updateGLBalance(String glNum, DrCrFlag drCrFlag, BigDecimal amount) {
        // Get GL balance with lock
        GLBalance balance = glBalanceRepository.findByGlNumWithLock(glNum)
                .orElseThrow(() -> new ResourceNotFoundException("GL Balance", "GL Number", glNum));

        BigDecimal oldBalance = balance.getCurrentBalance();
        BigDecimal newBalance;

        // Update balance based on debit/credit flag
        if (drCrFlag == DrCrFlag.D) {
            newBalance = oldBalance.add(amount); // Debit increases asset accounts
        } else {
            newBalance = oldBalance.subtract(amount); // Credit decreases asset accounts
        }

        // Update balance
        balance.setCurrentBalance(newBalance);
        // Replaced device-based date/time with System_Date (SystemDateService) - CBS Compliance Fix
        balance.setLastUpdated(systemDateService.getSystemDateTime());

        // Save updated balance
        glBalanceRepository.save(balance);

        log.info("GL balance updated for GL {}: {} {} {} = {}", 
                glNum, oldBalance, drCrFlag, amount, newBalance);

        return newBalance;
    }

    /**
     * Get current account balance
     * 
     * @param accountNo The account number
     * @return The account balance
     */
    public BigDecimal getAccountBalance(String accountNo) {
        return acctBalRepository.findLatestByAccountNo(accountNo)
                .map(AcctBal::getCurrentBalance)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Get computed available balance for an account
     * Formula: Balance = Opening_Bal + SUM(Credits) - SUM(Debits) from tran_table
     * 
     * @param accountNo The account number
     * @return The computed balance DTO
     */
    @Transactional(readOnly = true)
    public com.example.moneymarket.dto.AccountBalanceDTO getComputedAccountBalance(String accountNo) {
        // Replaced device-based date/time with System_Date (SystemDateService) - CBS Compliance Fix
        return getComputedAccountBalance(accountNo, systemDateService.getSystemDate());
    }

    /**
     * Get computed available balance for an account for a specific date
     * Formula: Balance = Previous Day Opening_Bal + SUM(Credits) - SUM(Debits) from current day transactions
     *
     * Uses 3-tier fallback logic for opening balance retrieval:
     * - Tier 1: Previous day's record (systemDate - 1)
     * - Tier 2: Last transaction date (MAX(Tran_Date) < systemDate)
     * - Tier 3: New account (return 0)
     *
     * @param accountNo The account number
     * @param systemDate The system date to use for calculations
     * @return The computed balance DTO
     */
    @Transactional(readOnly = true)
    public com.example.moneymarket.dto.AccountBalanceDTO getComputedAccountBalance(String accountNo, LocalDate systemDate) {
        // Get account balance from acct_bal for the specific date
        AcctBal currentDayBalance = acctBalRepository.findByAccountNoAndTranDate(accountNo, systemDate)
                .orElseGet(() -> acctBalRepository.findLatestByAccountNo(accountNo)
                        .orElseThrow(() -> new com.example.moneymarket.exception.ResourceNotFoundException(
                                "Account Balance", "Account Number", accountNo)));

        // Use shared 3-tier fallback logic to get previous day's opening balance
        BigDecimal previousDayOpeningBalance = accountBalanceUpdateService.getPreviousDayClosingBalance(accountNo, systemDate);

        // Get account details - try customer account first, then office account
        String accountName;
        String glNum = null;
        BigDecimal loanLimit = BigDecimal.ZERO;
        boolean isCustomerAccount = false;
        
        try {
            CustAcctMaster customerAccount = custAcctMasterRepository.findById(accountNo)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer Account", "Account Number", accountNo));
            accountName = customerAccount.getAcctName();
            glNum = customerAccount.getGlNum();
            loanLimit = customerAccount.getLoanLimit() != null ? customerAccount.getLoanLimit() : BigDecimal.ZERO;
            isCustomerAccount = true;
            
            // Force initialization of lazy-loaded entities if needed
            if (customerAccount.getSubProduct() != null) {
                customerAccount.getSubProduct().getSubProductId();
            }
            if (customerAccount.getCustomer() != null) {
                customerAccount.getCustomer().getCustId();
            }
        } catch (ResourceNotFoundException e) {
            // Try office account if customer account not found
            OFAcctMaster officeAccount = ofAcctMasterRepository.findById(accountNo)
                    .orElseThrow(() -> new ResourceNotFoundException("Account", "Account Number", accountNo));
            accountName = officeAccount.getAcctName();
            glNum = officeAccount.getGlNum();
            loanLimit = BigDecimal.ZERO; // Office accounts don't have loan limits
            isCustomerAccount = false;
            
            // Force initialization of lazy-loaded entities if needed
            if (officeAccount.getSubProduct() != null) {
                officeAccount.getSubProduct().getSubProductId();
            }
        }

        // Calculate debits and credits for the current system date
        BigDecimal dateDebits = tranTableRepository.sumDebitTransactionsForAccountOnDate(accountNo, systemDate)
                .orElse(BigDecimal.ZERO);

        BigDecimal dateCredits = tranTableRepository.sumCreditTransactionsForAccountOnDate(accountNo, systemDate)
                .orElse(BigDecimal.ZERO);

        // Compute balance: Previous Day Opening_Bal + Current Day Credits - Current Day Debits
        BigDecimal computedBalance = previousDayOpeningBalance
                .add(dateCredits)
                .subtract(dateDebits);

        // Calculate available balance based on account type
        // For Asset accounts (GL starting with "2"): Include loan limit
        // For Liability accounts (GL starting with "1"): No loan limit
        BigDecimal availableBalance;
        if (isCustomerAccount && glNum != null && glNum.startsWith("2")) {
            // Asset account: Available = Previous Day Opening + Loan Limit + Credits - Debits
            availableBalance = previousDayOpeningBalance
                    .add(loanLimit)
                    .add(dateCredits)
                    .subtract(dateDebits);
            
            log.debug("Asset account {} - Available balance includes loan limit: Previous Day Opening={}, Loan Limit={}, Credits={}, Debits={}, Available={}",
                    accountNo, previousDayOpeningBalance, loanLimit, dateCredits, dateDebits, availableBalance);
        } else {
            // Liability account or Office account: Available = Previous Day Opening
            availableBalance = previousDayOpeningBalance;
            
            log.debug("Liability/Office account {} - Available balance = Previous Day Opening: {}",
                    accountNo, availableBalance);
        }

        log.debug("Computed balance for account {} on date {}: Previous Day Opening={}, Current Day Debits={}, Current Day Credits={}, Computed={}",
                accountNo, systemDate, previousDayOpeningBalance, dateDebits, dateCredits, computedBalance);

        // Get interest accrued from acct_bal_accrual table (latest closing balance)
        BigDecimal interestAccrued = getLatestInterestAccrued(accountNo);

        return com.example.moneymarket.dto.AccountBalanceDTO.builder()
                .accountNo(accountNo)
                .accountName(accountName)
                .availableBalance(availableBalance)  // Updated to use calculated available balance (includes loan limit for Asset accounts)
                .currentBalance(currentDayBalance.getCurrentBalance())
                .todayDebits(dateDebits)
                .todayCredits(dateCredits)
                .computedBalance(computedBalance)
                .interestAccrued(interestAccrued)
                .build();
    }

    /**
     * Get latest interest accrued closing balance for an account
     * Fetches the most recent closing balance from acct_bal_accrual table
     * 
     * FIXED: Now uses direct native query to avoid JPA relationship issues
     * 
     * @param accountNo The account number
     * @return The latest interest accrued closing balance, or 0 if no records found
     */
    private BigDecimal getLatestInterestAccrued(String accountNo) {
        log.debug("Fetching latest interest accrued for account: {}", accountNo);
        
        try {
            // Use native query method to directly query by Account_No column
            Optional<AcctBalAccrual> latestAccrualOpt = acctBalAccrualRepository.findLatestByAccountNo(accountNo);
            
            if (latestAccrualOpt.isEmpty()) {
                log.debug("No interest accrual records found for account {} (with non-null Tran_date)", accountNo);
                return BigDecimal.ZERO;
            }

            AcctBalAccrual latestAccrual = latestAccrualOpt.get();
            BigDecimal closingBal = latestAccrual.getClosingBal();
            
            if (closingBal == null) {
                log.warn("Latest interest accrual record for account {} has null closing balance (Tran_date: {})", 
                        accountNo, latestAccrual.getTranDate());
                return BigDecimal.ZERO;
            }

            log.debug("Latest interest accrued for account {}: {} (from Tran_date: {}, Accrual_Date: {})",
                    accountNo, closingBal, latestAccrual.getTranDate(), latestAccrual.getAccrualDate());

            return closingBal;
        } catch (Exception e) {
            log.warn("Error fetching interest accrual for account {}: {}. Returning 0.", accountNo, e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * Get current GL balance
     * 
     * @param glNum The GL number
     * @return The GL balance
     */
    public BigDecimal getGLBalance(String glNum) {
        return glBalanceRepository.findLatestByGlNum(glNum)
                .map(GLBalance::getCurrentBalance)
                .orElse(BigDecimal.ZERO);
    }
}
