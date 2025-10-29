package com.example.moneymarket.service;

import com.example.moneymarket.entity.*;
import com.example.moneymarket.entity.InttAccrTran.AccrualStatus;
import com.example.moneymarket.entity.TranTable.DrCrFlag;
import com.example.moneymarket.exception.BusinessException;
import com.example.moneymarket.exception.ResourceNotFoundException;
import com.example.moneymarket.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for Batch Job 2: Interest Accrual Transaction Update
 * Implements comprehensive interest accrual with account type detection and rate lookup
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InterestAccrualService {

    private final CustAcctMasterRepository custAcctMasterRepository;
    private final SubProdMasterRepository subProdMasterRepository;
    private final AcctBalRepository acctBalRepository;
    private final InterestRateMasterRepository interestRateMasterRepository;
    private final InttAccrTranRepository inttAccrTranRepository;
    private final EODLogTableRepository eodLogTableRepository;
    private final SystemDateService systemDateService;

    /**
     * Batch Job 2: Interest Accrual Transaction Update
     *
     * @param accrualDate The accrual date (System_Date)
     * @return Number of accrual entries created (should be 2x number of accounts)
     */
    @Transactional
    public int runEODAccruals(LocalDate accrualDate) {
        LocalDate processDate = accrualDate != null ? accrualDate : systemDateService.getSystemDate();
        log.info("Starting Batch Job 2: Interest Accrual Transaction Update for date: {}", processDate);

        // Get all active customer accounts
        List<CustAcctMaster> activeAccounts = custAcctMasterRepository
                .findByAccountStatus(CustAcctMaster.AccountStatus.Active);

        if (activeAccounts.isEmpty()) {
            log.info("No active customer accounts found for accrual processing");
            return 0;
        }

        // Initialize sequential counter for custom ID generation
        int currentSequential = getNextAccrualSequentialNumber(processDate);
        log.info("Starting Accr_Tran_Id generation with sequential: {}", currentSequential);

        int totalEntriesCreated = 0;
        int accountsProcessed = 0;
        List<String> errors = new ArrayList<>();

        // Process each account
        for (CustAcctMaster account : activeAccounts) {
            try {
                int entriesCreated = processAccountAccrual(account, processDate, currentSequential);
                if (entriesCreated > 0) {
                    totalEntriesCreated += entriesCreated;
                    accountsProcessed++;
                    currentSequential++; // Increment after each account (not per entry)

                    if (accountsProcessed % 10 == 0) {
                        log.info("Processed {} accounts, current sequential: {}", accountsProcessed, currentSequential);
                    }
                }
            } catch (Exception e) {
                log.error("Error processing accrual for account {}: {}",
                        account.getAccountNo(), e.getMessage());
                errors.add(String.format("Account %s: %s", account.getAccountNo(), e.getMessage()));
            }
        }

        log.info("Batch Job 2 completed. Total entries created: {}, Errors: {}", totalEntriesCreated, errors.size());
        if (accountsProcessed > 0) {
            log.info("Generated {} accrual entries with sequential range: {} to {}",
                    totalEntriesCreated, getNextAccrualSequentialNumber(processDate), currentSequential - 1);
        }

        if (!errors.isEmpty()) {
            log.warn("Accrual process completed with {} errors: {}", errors.size(), String.join("; ", errors));
        }

        return totalEntriesCreated;
    }

    /**
     * Process accrual for a single account
     * Creates TWO entries in intt_accr_tran (one Debit, one Credit)
     *
     * @param account The account to process
     * @param accrualDate The accrual date
     * @param sequential The sequential number for ID generation
     * @return Number of entries created (0 if skipped, 2 if processed)
     * @throws BusinessException if accrual processing fails
     */
    private int processAccountAccrual(CustAcctMaster account, LocalDate accrualDate, int sequential) {
        String accountNo = account.getAccountNo();
        String glNum = account.getGlNum();

        // Determine account type: Deal vs Running
        AccountType accountType = determineAccountType(glNum);
        log.debug("Account {} type: {}, GL: {}", accountNo, accountType, glNum);

        // Get sub-product configuration
        SubProdMaster subProduct = account.getSubProduct();

        // Get effective interest rate based on account type
        BigDecimal effectiveInterestRate = getEffectiveInterestRate(subProduct, accountType, accrualDate);

        if (effectiveInterestRate == null || effectiveInterestRate.compareTo(BigDecimal.ZERO) == 0) {
            log.debug("Skipping accrual for account {} - no interest rate configured", accountNo);
            return 0;
        }

        // Get account balance (Closing_Bal from acct_bal after Batch Job 1)
        AcctBal accountBalance = acctBalRepository.findLatestByAccountNo(accountNo)
                .orElseThrow(() -> new ResourceNotFoundException("Account Balance", "Account Number", accountNo));

        BigDecimal closingBal = accountBalance.getClosingBal();
        if (closingBal == null || closingBal.compareTo(BigDecimal.ZERO) == 0) {
            log.debug("Skipping accrual for account {} - zero balance", accountNo);
            return 0;
        }

        // Calculate Accrued Interest: AI = (Account_Balance × Interest_Rate) / 36500
        BigDecimal accruedInterest = closingBal
                .multiply(effectiveInterestRate)
                .divide(new BigDecimal("36500"), 2, RoundingMode.HALF_UP);

        if (accruedInterest.compareTo(BigDecimal.ZERO) == 0) {
            log.debug("Skipping accrual for account {} - zero interest amount", accountNo);
            return 0;
        }

        // Determine if Liability or Asset account
        boolean isLiabilityAccount = glNum.startsWith("1");
        boolean isAssetAccount = glNum.startsWith("2");

        if (!isLiabilityAccount && !isAssetAccount) {
            log.warn("Skipping account {} - GL {} is neither Liability (1*) nor Asset (2*)", accountNo, glNum);
            return 0;
        }

        // Get consolidated GL account numbers from sub-product configuration
        String incomeExpenditureGLNum = subProduct.getInterestIncomeExpenditureGLNum();
        String receivablePayableGLNum = subProduct.getInterestReceivablePayableGLNum();

        // Validate at least ONE GL number is configured based on account type
        String debitGLAccount = null;
        String creditGLAccount = null;

        if (isLiabilityAccount) {
            // For LIABILITY accounts:
            // - incomeExpenditureGLNum stores EXPENDITURE GL (debit side)
            // - receivablePayableGLNum stores PAYABLE GL (credit side)
            boolean hasIncomeExp = (incomeExpenditureGLNum != null && !incomeExpenditureGLNum.trim().isEmpty());
            boolean hasRecvPay = (receivablePayableGLNum != null && !receivablePayableGLNum.trim().isEmpty());

            if (!hasIncomeExp && !hasRecvPay) {
                log.warn("Skipping liability account {} - no interest GL configured", accountNo);
                return 0;
            }

            // Select GL accounts for debit and credit entries
            // Debit entry: prefer expenditure GL, fallback to payable GL
            debitGLAccount = hasIncomeExp ? incomeExpenditureGLNum : receivablePayableGLNum;
            // Credit entry: prefer payable GL, fallback to expenditure GL
            creditGLAccount = hasRecvPay ? receivablePayableGLNum : incomeExpenditureGLNum;

        } else if (isAssetAccount) {
            // For ASSET accounts:
            // - incomeExpenditureGLNum stores INCOME GL (credit side)
            // - receivablePayableGLNum stores RECEIVABLE GL (debit side)
            boolean hasIncomeExp = (incomeExpenditureGLNum != null && !incomeExpenditureGLNum.trim().isEmpty());
            boolean hasRecvPay = (receivablePayableGLNum != null && !receivablePayableGLNum.trim().isEmpty());

            if (!hasIncomeExp && !hasRecvPay) {
                log.warn("Skipping asset account {} - no interest GL configured", accountNo);
                return 0;
            }

            // Select GL accounts for debit and credit entries
            // Debit entry: prefer receivable GL, fallback to income GL
            debitGLAccount = hasRecvPay ? receivablePayableGLNum : incomeExpenditureGLNum;
            // Credit entry: prefer income GL, fallback to receivable GL
            creditGLAccount = hasIncomeExp ? incomeExpenditureGLNum : receivablePayableGLNum;
        }

        // Generate custom Accr_Tran_Ids for the two entries
        String debitAccrTranId = generateAccrTranId(accrualDate, sequential, 1);
        String creditAccrTranId = generateAccrTranId(accrualDate, sequential, 2);

        int entriesCreated = 0;

        if (isLiabilityAccount) {
            // Liability Accounts: Create TWO entries, BOTH using customer account number
            // Entry 1: Dr customer account, GL_Account_No = debitGLAccount
            createAccrualEntry(debitAccrTranId, accountNo, accrualDate, effectiveInterestRate, accruedInterest,
                    DrCrFlag.D, debitGLAccount, "Interest Expenditure Accrual - " + accountNo);
            entriesCreated++;

            // Entry 2: Cr customer account, GL_Account_No = creditGLAccount
            createAccrualEntry(creditAccrTranId, accountNo, accrualDate, effectiveInterestRate, accruedInterest,
                    DrCrFlag.C, creditGLAccount, "Interest Payable Accrual - " + accountNo);
            entriesCreated++;

            log.info("Created liability accrual for account {}: Amount={}, Rate={}, Dr GL={}, Cr GL={}, IDs={}/{}",
                    accountNo, accruedInterest, effectiveInterestRate, debitGLAccount, creditGLAccount,
                    debitAccrTranId, creditAccrTranId);
        } else if (isAssetAccount) {
            // Asset Accounts: Create TWO entries, BOTH using customer account number
            // Entry 1: Dr customer account, GL_Account_No = debitGLAccount
            createAccrualEntry(debitAccrTranId, accountNo, accrualDate, effectiveInterestRate, accruedInterest,
                    DrCrFlag.D, debitGLAccount, "Interest Receivable Accrual - " + accountNo);
            entriesCreated++;

            // Entry 2: Cr customer account, GL_Account_No = creditGLAccount
            createAccrualEntry(creditAccrTranId, accountNo, accrualDate, effectiveInterestRate, accruedInterest,
                    DrCrFlag.C, creditGLAccount, "Interest Income Accrual - " + accountNo);
            entriesCreated++;

            log.info("Created asset accrual for account {}: Amount={}, Rate={}, Dr GL={}, Cr GL={}, IDs={}/{}",
                    accountNo, accruedInterest, effectiveInterestRate, debitGLAccount, creditGLAccount,
                    debitAccrTranId, creditAccrTranId);
        }

        return entriesCreated;
    }

    /**
     * Create a single interest accrual transaction entry with custom ID
     */
    private void createAccrualEntry(String accrTranId, String accountNo, LocalDate accrualDate,
                                   BigDecimal interestRate, BigDecimal amount, DrCrFlag drCrFlag,
                                   String glAccountNo, String narration) {
        // Validate ID format
        if (accrTranId == null || accrTranId.length() != 20) {
            throw new BusinessException("Invalid Accr_Tran_Id format: " + accrTranId +
                    " (expected 20 characters: S + YYYYMMDD + 9-digit + -row)");
        }

        // Create accrual entry
        InttAccrTran accrualEntry = InttAccrTran.builder()
                .accrTranId(accrTranId)  // Custom generated ID
                .accountNo(accountNo)
                .accrualDate(accrualDate)
                .tranDate(accrualDate)
                .valueDate(accrualDate)
                .interestRate(interestRate)
                .amount(amount)
                .drCrFlag(drCrFlag)
                .glAccountNo(glAccountNo)
                .tranCcy("BDT")  // Default currency
                .fcyAmt(amount)
                .exchangeRate(BigDecimal.ONE)
                .lcyAmt(amount)
                .narration(narration)
                .status(AccrualStatus.Pending)  // Set to Pending for Job 3 to process
                .tranStatus(TranTable.TranStatus.Verified)
                .build();

        log.debug("Generated Accr_Tran_Id: {} for Account: {}", accrTranId, accountNo);
        inttAccrTranRepository.save(accrualEntry);
    }

    /**
     * Determine account type based on GL pattern
     * Deal Accounts: 1102***** (Liability) and 2102***** (Asset)
     * Running Accounts: All other patterns
     */
    private AccountType determineAccountType(String glNum) {
        if (glNum == null || glNum.length() < 4) {
            return AccountType.RUNNING;
        }

        // Deal accounts have specific patterns
        if (glNum.startsWith("1102") || glNum.startsWith("2102")) {
            return AccountType.DEAL;
        }

        return AccountType.RUNNING;
    }

    /**
     * Get effective interest rate based on account type
     *
     * Running Accounts & Asset Deal Accounts:
     *   - Query interest_rate_master for latest rate where Intt_Effctv_Date <= System_Date
     *   - EIR = Base_Rate + Interest_Increment
     *
     * Liability Deal Accounts:
     *   - Use effective_interest_rate from sub_prod_master (rate at account opening)
     */
    private BigDecimal getEffectiveInterestRate(SubProdMaster subProduct, AccountType accountType, LocalDate asOfDate) {
        String inttCode = subProduct.getInttCode();

        if (inttCode == null || inttCode.trim().isEmpty()) {
            log.debug("No interest code configured for sub-product {}", subProduct.getSubProductCode());
            return BigDecimal.ZERO;
        }

        // Liability Deal Accounts: use fixed rate from sub-product
        if (accountType == AccountType.DEAL && subProduct.getCumGLNum() != null && subProduct.getCumGLNum().startsWith("1")) {
            BigDecimal fixedRate = subProduct.getEffectiveInterestRate();
            if (fixedRate != null) {
                log.debug("Using fixed rate {} for liability deal account", fixedRate);
                return fixedRate;
            }
        }

        // Running Accounts & Asset Deal Accounts: lookup from interest_rate_master
        Optional<InterestRateMaster> rateOpt = interestRateMasterRepository
                .findTopByInttCodeAndInttEffctvDateLessThanEqualOrderByInttEffctvDateDesc(inttCode, asOfDate);

        if (rateOpt.isEmpty()) {
            log.warn("No interest rate found for code {} as of {}", inttCode, asOfDate);
            return BigDecimal.ZERO;
        }

        BigDecimal baseRate = rateOpt.get().getInttRate();
        BigDecimal interestIncrement = subProduct.getInterestIncrement();

        if (interestIncrement == null) {
            interestIncrement = BigDecimal.ZERO;
        }

        // EIR = Base_Rate + Interest_Increment
        BigDecimal effectiveRate = baseRate.add(interestIncrement);
        log.debug("Calculated EIR: BaseRate={} + Increment={} = {}", baseRate, interestIncrement, effectiveRate);

        return effectiveRate;
    }

    /**
     * Account type enum
     */
    private enum AccountType {
        DEAL,       // Deal accounts: 1102***** or 2102*****
        RUNNING     // Running accounts: all others
    }

    // ==================================================================
    // Custom Accr_Tran_Id Generation Methods
    // ==================================================================

    /**
     * Get the next available sequential number for the accrual date
     * Queries database to find the maximum sequential number already used
     *
     * @param accrualDate The accrual date
     * @return Next sequential number (1 if no records exist for this date)
     */
    private int getNextAccrualSequentialNumber(LocalDate accrualDate) {
        try {
            Optional<Integer> maxSeqOpt = inttAccrTranRepository.findMaxSequentialByAccrualDate(accrualDate);
            int nextSeq = maxSeqOpt.map(max -> max + 1).orElse(1);

            if (nextSeq > 999999999) {
                log.warn("Sequential number {} exceeds maximum (999999999) for date {}", nextSeq, accrualDate);
            }

            log.debug("Next sequential number for {}: {}", accrualDate, nextSeq);
            return nextSeq;
        } catch (Exception e) {
            log.error("Error retrieving max sequential number for date {}: {}", accrualDate, e.getMessage());
            throw new BusinessException("Failed to generate sequential number: " + e.getMessage());
        }
    }

    /**
     * Format date for Accr_Tran_Id (YYYYMMDD format)
     *
     * @param date The date to format
     * @return Formatted date string (8 characters)
     */
    private String formatDateForAccrId(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        return date.format(formatter);
    }

    /**
     * Zero-pad sequential number to 9 digits
     *
     * @param sequential The sequential number (1-999999999)
     * @return Zero-padded string (e.g., "000000001")
     */
    private String zeroPadSequential(int sequential) {
        if (sequential < 1 || sequential > 999999999) {
            throw new BusinessException("Sequential number out of range: " + sequential +
                    " (must be 1-999999999)");
        }
        return String.format("%09d", sequential);
    }

    /**
     * Generate Accr_Tran_Id in format: S + YYYYMMDD + 9-digit-sequential + -row-suffix
     * Examples:
     *   S20251020000000001-1  (First account, debit entry)
     *   S20251020000000001-2  (First account, credit entry)
     *   S20251020000000002-1  (Second account, debit entry)
     *
     * @param accrualDate The accrual date
     * @param sequential The sequential number for this account (1-based)
     * @param rowSuffix The row suffix (1 for debit, 2 for credit)
     * @return Generated Accr_Tran_Id (20 characters)
     */
    private String generateAccrTranId(LocalDate accrualDate, int sequential, int rowSuffix) {
        if (accrualDate == null) {
            throw new BusinessException("Accrual date cannot be null for ID generation");
        }
        if (rowSuffix != 1 && rowSuffix != 2) {
            throw new BusinessException("Row suffix must be 1 (debit) or 2 (credit), got: " + rowSuffix);
        }

        String formattedDate = formatDateForAccrId(accrualDate);
        String paddedSeq = zeroPadSequential(sequential);
        String accrTranId = "S" + formattedDate + paddedSeq + "-" + rowSuffix;

        // Validate final ID length
        if (accrTranId.length() != 20) {
            throw new BusinessException("Generated Accr_Tran_Id has invalid length: " + accrTranId +
                    " (expected 20, got " + accrTranId.length() + ")");
        }

        return accrTranId;
    }

}
