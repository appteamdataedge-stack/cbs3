package com.example.moneymarket.service;

import com.example.moneymarket.entity.AcctBalAccrual;
import com.example.moneymarket.entity.CustAcctMaster;
import com.example.moneymarket.entity.SubProdMaster;
import com.example.moneymarket.exception.BusinessException;
import com.example.moneymarket.repository.AcctBalAccrualRepository;
import com.example.moneymarket.repository.CustAcctMasterRepository;
import com.example.moneymarket.repository.InttAccrTranRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Service for Batch Job 6: Interest Accrual Account Balance Update
 * Processes interest accrual transactions into account balance accrual records
 * with GL_Num-based conditional DR/CR summation logic
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InterestAccrualAccountBalanceService {

    private final InttAccrTranRepository inttAccrTranRepository;
    private final AcctBalAccrualRepository acctBalAccrualRepository;
    private final CustAcctMasterRepository custAcctMasterRepository;
    private final SystemDateService systemDateService;

    /**
     * Batch Job 6: Interest Accrual Account Balance Update
     *
     * Process:
     * 1. Get unique Account_No from intt_accr_tran where Accrual_Date = System_Date
     *
     * 2. For each Account_No:
     *    a. Get GL_Num from account's sub-product
     *    b. Determine account type (Liability=1, Asset=2)
     *    c. Get Opening_Bal from previous day's Closing_Bal
     *    d. Calculate DR_Summation (conditional based on GL_Num)
     *    e. Calculate CR_Summation (conditional based on GL_Num)
     *    f. Calculate Closing_Bal = Opening_Bal + CR_Summation - DR_Summation
     *    g. Insert/Update acct_bal_accrual with GL_Num
     *
     * @param systemDate The system date for processing
     * @return Number of accounts processed
     */
    @Transactional
    public int updateInterestAccrualAccountBalances(LocalDate systemDate) {
        LocalDate processDate = systemDate != null ? systemDate : systemDateService.getSystemDate();
        log.info("Starting Batch Job 6: Interest Accrual Account Balance Update for date: {}", processDate);

        // Step 1: Get unique account numbers from interest accrual transactions
        List<String> accountNumbers = inttAccrTranRepository.findDistinctAccountsByAccrualDate(processDate);

        if (accountNumbers.isEmpty()) {
            log.info("No interest accrual transactions found for date: {}", processDate);
            return 0;
        }

        log.info("Found {} unique accounts with interest accruals", accountNumbers.size());

        int recordsProcessed = 0;
        List<String> errors = new ArrayList<>();

        // Step 2: Process each account
        for (String accountNo : accountNumbers) {
            try {
                processAccountAccrualBalance(accountNo, processDate);
                recordsProcessed++;
            } catch (Exception e) {
                log.error("Error processing accrual balance for account {}: {}", accountNo, e.getMessage(), e);
                errors.add(String.format("Account %s: %s", accountNo, e.getMessage()));
            }
        }

        log.info("Batch Job 6 completed. Records processed: {}, Errors: {}",
                recordsProcessed, errors.size());

        if (!errors.isEmpty()) {
            log.warn("Accrual account balance process completed with {} errors: {}",
                    errors.size(), String.join("; ", errors));
        }

        return recordsProcessed;
    }

    /**
     * Process accrual balance for a single account with GL_Num-based logic
     */
    private void processAccountAccrualBalance(String accountNo, LocalDate systemDate) {

        // Step A: Get GL_Num from account's sub-product
        String glNum = getGLNumberForAccount(accountNo);
        log.debug("Account {}: GL_Num = {}", accountNo, glNum);

        // Step B: Determine account type
        boolean isLiability = glNum.startsWith("1");
        boolean isAsset = glNum.startsWith("2");

        if (!isLiability && !isAsset) {
            throw new BusinessException("Invalid GL_Num: " + glNum +
                    " - must start with '1' (Liability) or '2' (Asset)");
        }

        String accountType = isLiability ? "LIABILITY" : "ASSET";
        log.debug("Account {}: Type = {}", accountNo, accountType);

        // Step C: Get Opening Balance
        BigDecimal openingBal = getOpeningBalance(accountNo, systemDate);

        // Step D: Calculate DR Summation (Conditional based on GL_Num)
        BigDecimal drSummation = calculateDRSummation(accountNo, systemDate, glNum);

        // Step E: Calculate CR Summation (Conditional based on GL_Num)
        BigDecimal crSummation = calculateCRSummation(accountNo, systemDate, glNum);

        // Validation: One of DR or CR must be zero
        if (drSummation.compareTo(BigDecimal.ZERO) > 0 && crSummation.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("Validation failed for account " + accountNo +
                    ": Both DR and CR summations are non-zero. GL_Num: " + glNum);
        }

        // Step F: Calculate Closing Balance
        // Closing_Bal = Opening_Bal + CR_Summation - DR_Summation
        BigDecimal closingBal = openingBal.add(crSummation).subtract(drSummation);

        // Step G: Calculate Interest Amount
        // Interest_Amount = CR_Summation - DR_Summation
        BigDecimal interestAmount = crSummation.subtract(drSummation);

        log.debug("Account {}: Opening={}, DR={}, CR={}, Closing={}, Interest={}",
                accountNo, openingBal, drSummation, crSummation, closingBal, interestAmount);

        // Step H: Save Record with GL_Num
        saveOrUpdateAccrualBalance(accountNo, glNum, systemDate, openingBal, drSummation,
                crSummation, closingBal, interestAmount);
    }

    /**
     * Get GL Number for Account
     * Query path: cust_acct_master -> Sub_Product_Id -> sub_prod_master -> Cum_GL_Num
     *
     * @param accountNo The account number
     * @return GL_Num from sub-product (9 characters)
     */
    private String getGLNumberForAccount(String accountNo) {
        // Step 1: Get account with sub-product
        CustAcctMaster account = custAcctMasterRepository.findById(accountNo)
                .orElseThrow(() -> new BusinessException("Account not found: " + accountNo));

        // Step 2: Get sub-product
        SubProdMaster subProduct = account.getSubProduct();
        if (subProduct == null) {
            throw new BusinessException("Sub-product not found for account: " + accountNo);
        }

        // Step 3: Get Cum_GL_Num
        String cumGlNum = subProduct.getCumGLNum();
        if (cumGlNum == null || cumGlNum.trim().isEmpty()) {
            throw new BusinessException("Cum_GL_Num is null or empty for account: " + accountNo +
                    ", Sub-Product: " + subProduct.getSubProductId());
        }

        // Validate GL_Num length
        if (cumGlNum.length() != 9) {
            throw new BusinessException("Invalid GL_Num length: " + cumGlNum +
                    " (expected 9 characters) for account: " + accountNo);
        }

        return cumGlNum;
    }

    /**
     * Calculate DR Summation (Conditional based on GL_Num)
     *
     * Logic:
     * - IF GL_Num starts with "1" (Liability) -> return 0
     * - IF GL_Num starts with "2" (Asset) -> return SUM(Amount) WHERE Dr_Cr_Flag = 'D'
     *
     * @param accountNo The account number
     * @param accrualDate The accrual date
     * @param glNum The GL number
     * @return DR_Summation
     */
    private BigDecimal calculateDRSummation(String accountNo, LocalDate accrualDate, String glNum) {
        if (glNum.startsWith("1")) {
            // Liability accounts: DR_Summation = 0
            log.debug("Account {}: Liability account - DR_Summation set to 0", accountNo);
            return BigDecimal.ZERO;
        } else if (glNum.startsWith("2")) {
            // Asset accounts: Calculate sum of debit amounts
            BigDecimal drSum = inttAccrTranRepository.sumDebitAmountsByAccountAndDate(accountNo, accrualDate);
            log.debug("Account {}: Asset account - DR_Summation = {}", accountNo, drSum);
            return drSum;
        } else {
            throw new BusinessException("Invalid GL_Num: " + glNum + " - must start with '1' or '2'");
        }
    }

    /**
     * Calculate CR Summation (Conditional based on GL_Num)
     *
     * Logic:
     * - IF GL_Num starts with "2" (Asset) -> return 0
     * - IF GL_Num starts with "1" (Liability) -> return SUM(Amount) WHERE Dr_Cr_Flag = 'C'
     *
     * @param accountNo The account number
     * @param accrualDate The accrual date
     * @param glNum The GL number
     * @return CR_Summation
     */
    private BigDecimal calculateCRSummation(String accountNo, LocalDate accrualDate, String glNum) {
        if (glNum.startsWith("2")) {
            // Asset accounts: CR_Summation = 0
            log.debug("Account {}: Asset account - CR_Summation set to 0", accountNo);
            return BigDecimal.ZERO;
        } else if (glNum.startsWith("1")) {
            // Liability accounts: Calculate sum of credit amounts
            BigDecimal crSum = inttAccrTranRepository.sumCreditAmountsByAccountAndDate(accountNo, accrualDate);
            log.debug("Account {}: Liability account - CR_Summation = {}", accountNo, crSum);
            return crSum;
        } else {
            throw new BusinessException("Invalid GL_Num: " + glNum + " - must start with '1' or '2'");
        }
    }

    /**
     * Get opening balance for account
     * Opening balance = previous day's closing balance
     */
    /**
     * Get opening balance for account using 3-tier fallback logic
     *
     * This method implements the standardized 3-tier fallback logic for opening balance retrieval:
     * - Tier 1: Previous day's record (systemDate - 1)
     * - Tier 2: Last transaction date (MAX(Tran_Date) < systemDate)
     * - Tier 3: New account (return 0)
     *
     * This ensures consistency with Batch Job 1 (Account Balance Update) and Batch Job 5 (GL Balance Update) logic.
     *
     * @param accountNo The account number
     * @param systemDate The system date
     * @return Opening balance (previous day's closing balance or last available closing balance)
     */
    private BigDecimal getOpeningBalance(String accountNo, LocalDate systemDate) {
        // Get all account balance accrual records for this account before the system date
        List<AcctBalAccrual> acctBalances = acctBalAccrualRepository
                .findByAccountAccountNoAndTranDateBeforeOrderByTranDateDesc(accountNo, systemDate);

        // Tier 3: If no previous record exists at all, Opening_Bal = 0 (new account or first accrual)
        if (acctBalances.isEmpty()) {
            log.info("3-Tier Fallback [Tier 3 - New Account/First Accrual]: Account {} has no previous accrual records before {}. Using Opening_Bal = 0",
                    accountNo, systemDate);
            return BigDecimal.ZERO;
        }

        // Tier 1: Try to get the previous day's record
        LocalDate previousDay = systemDate.minusDays(1);
        Optional<AcctBalAccrual> previousDayRecord = acctBalances.stream()
                .filter(acctBal -> previousDay.equals(acctBal.getTranDate()))
                .findFirst();

        if (previousDayRecord.isPresent()) {
            BigDecimal previousDayClosingBal = previousDayRecord.get().getClosingBal();
            if (previousDayClosingBal == null) {
                previousDayClosingBal = BigDecimal.ZERO;
            }
            log.debug("3-Tier Fallback [Tier 1 - Previous Day]: Account {} found accrual record for {} with Closing_Bal = {}",
                    accountNo, previousDay, previousDayClosingBal);
            return previousDayClosingBal;
        }

        // Tier 2: Previous day's record doesn't exist, use last transaction date
        AcctBalAccrual lastRecord = acctBalances.get(0); // First in sorted list (most recent)
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
     * Save or update account accrual balance record with GL_Num
     */
    private void saveOrUpdateAccrualBalance(String accountNo, String glNum, LocalDate tranDate,
                                           BigDecimal openingBal, BigDecimal drSummation,
                                           BigDecimal crSummation, BigDecimal closingBal,
                                           BigDecimal interestAmount) {
        // Check if balance record already exists for this account and date
        Optional<AcctBalAccrual> existingBalanceOpt = acctBalAccrualRepository
                .findByAccountAccountNoAndTranDate(accountNo, tranDate);

        AcctBalAccrual acctBalAccrual;

        if (existingBalanceOpt.isPresent()) {
            // Update existing record
            acctBalAccrual = existingBalanceOpt.get();
            acctBalAccrual.setGlNum(glNum);
            acctBalAccrual.setOpeningBal(openingBal);
            acctBalAccrual.setDrSummation(drSummation);
            acctBalAccrual.setCrSummation(crSummation);
            acctBalAccrual.setClosingBal(closingBal);
            acctBalAccrual.setInterestAmount(interestAmount);
            acctBalAccrual.setAccrualDate(tranDate);
            log.debug("Updated existing accrual balance for account {} with GL_Num {}", accountNo, glNum);
        } else {
            // Get the account entity for the foreign key relationship
            CustAcctMaster account = custAcctMasterRepository.findById(accountNo)
                    .orElseThrow(() -> new RuntimeException("Account not found: " + accountNo));

            // Create new record
            acctBalAccrual = AcctBalAccrual.builder()
                    .account(account)  // Set the required foreign key
                    .glNum(glNum)      // NEW: Set GL_Num
                    .tranDate(tranDate)
                    .accrualDate(tranDate)
                    .openingBal(openingBal)
                    .drSummation(drSummation)
                    .crSummation(crSummation)
                    .closingBal(closingBal)
                    .interestAmount(interestAmount)
                    .build();

            log.debug("Created new accrual balance for account {} with GL_Num {}", accountNo, glNum);
        }

        acctBalAccrualRepository.save(acctBalAccrual);
    }
}
