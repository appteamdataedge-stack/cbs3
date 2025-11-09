package com.example.moneymarket.service;

import com.example.moneymarket.entity.*;
import com.example.moneymarket.exception.BusinessException;
import com.example.moneymarket.exception.ResourceNotFoundException;
import com.example.moneymarket.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Service for posting interest adjustments for value-dated transactions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ValueDatePostingService {

    private final TranValueDateLogRepository tranValueDateLogRepository;
    private final GLMovementRepository glMovementRepository;
    private final GLSetupRepository glSetupRepository;
    private final CustAcctMasterRepository custAcctMasterRepository;
    private final OFAcctMasterRepository ofAcctMasterRepository;
    private final TranTableRepository tranTableRepository;
    private final BalanceService balanceService;
    private final UnifiedAccountService unifiedAccountService;
    private final SystemDateService systemDateService;

    /**
     * Log a value-dated transaction
     *
     * @param tranId The transaction ID
     * @param valueDate The value date
     * @param daysDifference The days difference (positive for past, negative for future)
     * @param deltaInterest The calculated delta interest amount
     * @param adjustmentPostedFlag 'Y' if posted immediately (past-dated), 'N' if pending (future-dated)
     */
    @Transactional
    public void logValueDateTransaction(String tranId, LocalDate valueDate, int daysDifference,
                                       BigDecimal deltaInterest, String adjustmentPostedFlag) {
        TranValueDateLog logEntry = TranValueDateLog.builder()
            .tranId(tranId)
            .valueDate(valueDate)
            .daysDifference(daysDifference)
            .deltaInterestAmt(deltaInterest != null ? deltaInterest : BigDecimal.ZERO)
            .adjustmentPostedFlag(adjustmentPostedFlag)
            .createdTimestamp(systemDateService.getSystemDateTime())
            .build();

        tranValueDateLogRepository.save(logEntry);
        log.info("Value date transaction logged: TranId={}, ValueDate={}, DaysDiff={}, DeltaInterest={}, Posted={}",
            tranId, valueDate, daysDifference, deltaInterest, adjustmentPostedFlag);
    }

    /**
     * Post interest adjustment entries for a past-dated transaction
     * Creates GL entries based on account type (liability vs asset) and transaction direction
     *
     * @param transaction The main transaction
     * @param deltaInterest The delta interest amount
     */
    @Transactional
    public void postInterestAdjustment(TranTable transaction, BigDecimal deltaInterest) {
        if (deltaInterest.compareTo(BigDecimal.ZERO) == 0) {
            log.debug("Delta interest is zero for transaction {}. Skipping adjustment posting.", transaction.getTranId());
            return;
        }

        String accountNo = transaction.getAccountNo();
        String glNum = unifiedAccountService.getGlNum(accountNo);

        // Determine account type based on GL number
        boolean isLiability = glNum.startsWith("1");
        boolean isAsset = glNum.startsWith("2");

        if (!isLiability && !isAsset) {
            throw new BusinessException("Invalid GL number for interest adjustment: " + glNum +
                ". GL must start with '1' (liability) or '2' (asset)");
        }

        // Get interest GL accounts from sub-product
        SubProdMaster subProduct = getSubProduct(accountNo);
        String drGLAccount;
        String crGLAccount;

        if (isLiability) {
            // LIABILITY ACCOUNT (Deposits, Savings - GL 1xxxxx)
            if (transaction.getDrCrFlag() == TranTable.DrCrFlag.C) {
                // Deposit (Credit): Bank owes MORE interest
                // Dr Interest Expense GL
                // Cr Accrued Interest Payable GL
                drGLAccount = subProduct.getInterestReceivableExpenditureGLNum(); // Expenditure GL
                crGLAccount = subProduct.getInterestIncomePayableGLNum(); // Payable GL
            } else {
                // Withdrawal (Debit): Bank owes LESS interest (reverse)
                // Dr Accrued Interest Payable GL
                // Cr Interest Expense GL
                drGLAccount = subProduct.getInterestIncomePayableGLNum(); // Payable GL
                crGLAccount = subProduct.getInterestReceivableExpenditureGLNum(); // Expenditure GL
            }
        } else {
            // ASSET ACCOUNT (Loans, Overdrafts - GL 2xxxxx)
            if (transaction.getDrCrFlag() == TranTable.DrCrFlag.D) {
                // Disbursement (Debit): Bank earns MORE interest
                // Dr Accrued Interest Receivable GL
                // Cr Interest Income GL
                drGLAccount = subProduct.getInterestReceivableExpenditureGLNum(); // Receivable GL
                crGLAccount = subProduct.getInterestIncomePayableGLNum(); // Income GL
            } else {
                // Repayment (Credit): Bank earns LESS interest (reverse)
                // Dr Interest Income GL
                // Cr Accrued Interest Receivable GL
                drGLAccount = subProduct.getInterestIncomePayableGLNum(); // Income GL
                crGLAccount = subProduct.getInterestReceivableExpenditureGLNum(); // Receivable GL
            }
        }

        // Validate GL accounts exist
        if (drGLAccount == null || crGLAccount == null) {
            throw new BusinessException(
                "Interest GL accounts not configured for account " + accountNo +
                ". Please configure interest_receivable_expenditure_gl_num and interest_income_payable_gl_num in sub-product master."
            );
        }

        // Post the two adjustment entries
        postGLEntry(transaction, drGLAccount, TranTable.DrCrFlag.D, deltaInterest, "Value Date Interest Adjustment - Debit");
        postGLEntry(transaction, crGLAccount, TranTable.DrCrFlag.C, deltaInterest, "Value Date Interest Adjustment - Credit");

        log.info("Interest adjustment posted for transaction {}: Dr GL={}, Cr GL={}, Amount={}",
            transaction.getTranId(), drGLAccount, crGLAccount, deltaInterest);
    }

    /**
     * Post a GL entry for interest adjustment
     */
    private void postGLEntry(TranTable originalTransaction, String glAccount, TranTable.DrCrFlag drCrFlag,
                            BigDecimal amount, String narration) {
        // Verify GL exists
        GLSetup glSetup = glSetupRepository.findById(glAccount)
            .orElseThrow(() -> new ResourceNotFoundException("GL Setup", "GL Number", glAccount));

        // Update GL balance
        BigDecimal newGLBalance = balanceService.updateGLBalance(glAccount, drCrFlag, amount);

        // Create GL movement record
        GLMovement glMovement = GLMovement.builder()
            .transaction(originalTransaction)
            .glSetup(glSetup)
            .drCrFlag(drCrFlag)
            .tranDate(originalTransaction.getTranDate())
            .valueDate(originalTransaction.getValueDate())
            .amount(amount)
            .balanceAfter(newGLBalance)
            .build();

        glMovementRepository.save(glMovement);

        log.debug("GL entry posted: GL={}, DrCr={}, Amount={}, Balance={}",
            glAccount, drCrFlag, amount, newGLBalance);
    }

    /**
     * Get sub-product for an account (customer or office)
     */
    private SubProdMaster getSubProduct(String accountNo) {
        // Try customer account first
        try {
            CustAcctMaster custAccount = custAcctMasterRepository.findById(accountNo)
                .orElseThrow(() -> new ResourceNotFoundException("Customer Account", "Account Number", accountNo));
            return custAccount.getSubProduct();
        } catch (ResourceNotFoundException e) {
            // Try office account
            OFAcctMaster officeAccount = ofAcctMasterRepository.findById(accountNo)
                .orElseThrow(() -> new ResourceNotFoundException("Office Account", "Account Number", accountNo));
            return officeAccount.getSubProduct();
        }
    }

    /**
     * Update the adjustment posted flag in the log
     *
     * @param tranId The transaction ID
     * @param flag The flag value ('Y' or 'N')
     */
    @Transactional
    public void updateAdjustmentPostedFlag(String tranId, String flag) {
        tranValueDateLogRepository.updateAdjustmentPostedFlag(tranId, flag);
        log.debug("Updated adjustment posted flag for transaction {}: {}", tranId, flag);
    }
}
