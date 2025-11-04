package com.example.moneymarket.service;

import com.example.moneymarket.entity.*;
import com.example.moneymarket.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing transaction history (TXN_HIST_ACCT)
 * Populates transaction history in real-time when transactions are verified
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionHistoryService {

    private final TxnHistAcctRepository txnHistAcctRepository;
    private final CustAcctMasterRepository custAcctMasterRepository;
    private final OFAcctMasterRepository ofAcctMasterRepository;
    private final SubProdMasterRepository subProdMasterRepository;
    private final AcctBalRepository acctBalRepository;
    private final SystemDateService systemDateService;

    /**
     * Create transaction history record when a transaction is verified
     * This method is called for each transaction line (debit or credit leg)
     *
     * @param transaction The verified transaction
     * @param verifierUserId The user who verified the transaction
     */
    @Transactional
    public void createTransactionHistory(TranTable transaction, String verifierUserId) {
        try {
            log.debug("Creating transaction history for account {}, Tran_ID {}", 
                    transaction.getAccountNo(), transaction.getTranId());

            // Step 1: Get Transaction Details
            String accountNo = transaction.getAccountNo();
            LocalDate tranDate = transaction.getTranDate();
            LocalDate valueDate = transaction.getValueDate();
            String tranId = transaction.getTranId();
            String narration = transaction.getNarration();
            BigDecimal tranAmt = transaction.getLcyAmt();
            TranTable.DrCrFlag drCrFlag = transaction.getDrCrFlag();

            // Step 2: Determine Account Type and Get Details
            AccountDetails accountDetails = getAccountDetails(accountNo);
            if (accountDetails == null) {
                log.warn("Account not found for transaction history: {}", accountNo);
                return; // Don't fail transaction verification
            }

        // Step 3: Get Opening Balance based on whether this is FIRST or SUBSEQUENT transaction of the day
        BigDecimal openingBalance = getOpeningBalanceForTransaction(accountNo, tranDate);

        // Step 4: Calculate Balance After Transaction
        TxnHistAcct.TransactionType tranType = drCrFlag == TranTable.DrCrFlag.D
                ? TxnHistAcct.TransactionType.D
                : TxnHistAcct.TransactionType.C;

        BigDecimal balanceAfterTran;
        if (tranType == TxnHistAcct.TransactionType.C) {
            // Credit increases balance
            balanceAfterTran = openingBalance.add(tranAmt);
        } else {
            // Debit decreases balance
            balanceAfterTran = openingBalance.subtract(tranAmt);
        }

            // Step 5: Determine TRAN_SL_NO
            Integer tranSlNo = getNextSerialNumber(tranId);

        // Step 6: Create TxnHistAcct Record
        TxnHistAcct histRecord = new TxnHistAcct();
        histRecord.setBranchId(accountDetails.branchId);
        histRecord.setAccNo(accountNo);
        histRecord.setTranId(tranId);
        histRecord.setTranDate(tranDate);
        histRecord.setValueDate(valueDate);
        histRecord.setTranSlNo(tranSlNo);
        histRecord.setNarration(narration != null ? narration.substring(0, Math.min(narration.length(), 100)) : null);
        histRecord.setTranType(tranType);
        histRecord.setTranAmt(tranAmt);
        histRecord.setOpeningBalance(openingBalance);
        histRecord.setBalanceAfterTran(balanceAfterTran);
        histRecord.setEntryUserId(transaction.getUdf1() != null ? transaction.getUdf1() : "SYSTEM");
        histRecord.setAuthUserId(verifierUserId);
        histRecord.setCurrencyCode("BDT");
        histRecord.setGlNum(accountDetails.glNum);
        histRecord.setRcreDate(systemDateService.getSystemDate());
        histRecord.setRcreTime(LocalTime.now());

        // Step 7: Save Record
        txnHistAcctRepository.save(histRecord);
        log.info("Transaction history created successfully for account {}, Tran_ID {}, Opening Balance: {}, Transaction Amount: {} ({}), New Balance: {}",
                accountNo, tranId, openingBalance, tranAmt, tranType, balanceAfterTran);

        } catch (Exception e) {
            // Step 8: Handle Errors - Log warning but don't fail transaction verification
            log.warn("Error creating transaction history for account {}, Tran_ID {}: {}. Transaction verification will continue.", 
                    transaction.getAccountNo(), transaction.getTranId(), e.getMessage(), e);
        }
    }

    /**
     * Get account details (branch, GL number, etc.)
     */
    private AccountDetails getAccountDetails(String accountNo) {
        // Try customer account first
        Optional<CustAcctMaster> custAccount = custAcctMasterRepository.findById(accountNo);
        if (custAccount.isPresent()) {
            CustAcctMaster account = custAccount.get();
            String glNum = null;
            if (account.getSubProduct() != null) {
                SubProdMaster subProduct = subProdMasterRepository.findById(account.getSubProduct().getSubProductId())
                        .orElse(null);
                if (subProduct != null) {
                    glNum = subProduct.getCumGLNum();
                }
            }
            return new AccountDetails(
                    account.getBranchCode() != null ? account.getBranchCode() : "DEFAULT",
                    glNum
            );
        }

        // Try office account
        Optional<OFAcctMaster> officeAccount = ofAcctMasterRepository.findById(accountNo);
        if (officeAccount.isPresent()) {
            OFAcctMaster account = officeAccount.get();
            String glNum = null;
            if (account.getSubProduct() != null) {
                SubProdMaster subProduct = subProdMasterRepository.findById(account.getSubProduct().getSubProductId())
                        .orElse(null);
                if (subProduct != null) {
                    glNum = subProduct.getCumGLNum();
                }
            }
            return new AccountDetails(
                    account.getBranchCode() != null ? account.getBranchCode() : "DEFAULT",
                    glNum
            );
        }

        return null;
    }

    /**
     * Get opening balance for a transaction based on user's specified logic:
     *
     * For FIRST transaction of an account on a given day:
     * - Get the account's last closing balance from acct_bal table (from most recent date)
     * - This will be the opening balance for the first transaction
     *
     * For SECOND, THIRD, and subsequent transactions of the SAME account on the SAME day:
     * - Get the previous transaction's BALANCE_AFTER_TRAN from txn_hist_acct
     * - This will be the opening balance for the current transaction
     *
     * @param accountNo The account number
     * @param tranDate The transaction date
     * @return Opening balance for this transaction
     */
    private BigDecimal getOpeningBalanceForTransaction(String accountNo, LocalDate tranDate) {
        // Check if there are any transactions for this account on the SAME day already recorded
        List<TxnHistAcct> todayTransactions = txnHistAcctRepository.findByAccNoAndTranDateOrderByRcreTimeAsc(
                accountNo, tranDate);

        if (!todayTransactions.isEmpty()) {
            // This is NOT the first transaction of the day
            // Get the most recent transaction's BALANCE_AFTER_TRAN
            TxnHistAcct lastTransactionToday = todayTransactions.get(todayTransactions.size() - 1);
            BigDecimal openingBalance = lastTransactionToday.getBalanceAfterTran();

            log.debug("Found {} transaction(s) today for account {}. Using previous transaction's closing balance: {}",
                    todayTransactions.size(), accountNo, openingBalance);

            return openingBalance;
        } else {
            // This is the FIRST transaction of the day
            // Get the last closing balance from acct_bal table
            BigDecimal closingBalance = getLastClosingBalanceFromAcctBal(accountNo, tranDate);

            log.debug("First transaction of the day for account {}. Using last closing balance from acct_bal: {}",
                    accountNo, closingBalance);

            return closingBalance;
        }
    }

    /**
     * Get last closing balance from acct_bal table (most recent date before or on tranDate)
     * This is used for the FIRST transaction of the day
     *
     * Fallback logic:
     * 1. Try to get closing_bal from acct_bal for the most recent date <= tranDate
     * 2. If closing_bal is null, use current_balance
     * 3. If no record found in acct_bal, check txn_hist_acct for last transaction
     * 4. Default to 0 if nothing found
     *
     * @param accountNo The account number
     * @param tranDate The transaction date
     * @return Last closing balance
     */
    private BigDecimal getLastClosingBalanceFromAcctBal(String accountNo, LocalDate tranDate) {
        // Try to get the most recent balance from acct_bal (on or before tranDate)
        Optional<AcctBal> latestBalance = acctBalRepository.findLatestByAccountNo(accountNo);

        if (latestBalance.isPresent()) {
            AcctBal acctBal = latestBalance.get();

            // Prefer closing_bal if available, otherwise use current_balance
            BigDecimal balance = acctBal.getClosingBal() != null
                    ? acctBal.getClosingBal()
                    : acctBal.getCurrentBalance();

            log.debug("Found acct_bal record for account {} on date {}. Using closing balance: {}",
                    accountNo, acctBal.getTranDate(), balance);

            return balance != null ? balance : BigDecimal.ZERO;
        }

        // Fallback: Try to get from txn_hist_acct if no acct_bal record exists
        Optional<TxnHistAcct> lastTransaction = txnHistAcctRepository.findLastTransactionBeforeDate(
                accountNo, tranDate.plusDays(1)); // +1 to include same day if checking before first tran

        if (lastTransaction.isPresent()) {
            BigDecimal balance = lastTransaction.get().getBalanceAfterTran();
            log.debug("No acct_bal found for account {}. Using last transaction balance: {}",
                    accountNo, balance);
            return balance;
        }

        // Default to 0 for new accounts
        log.debug("No previous balance found for account {} (new account). Using opening balance: 0", accountNo);
        return BigDecimal.ZERO;
    }

    /**
     * Get next serial number for multi-leg transactions
     */
    private Integer getNextSerialNumber(String tranId) {
        Integer maxSlNo = txnHistAcctRepository.findMaxTranSlNoByTranId(tranId);
        return maxSlNo != null ? maxSlNo + 1 : 1;
    }

    /**
     * Helper class to hold account details
     */
    private static class AccountDetails {
        String branchId;
        String glNum;

        AccountDetails(String branchId, String glNum) {
            this.branchId = branchId;
            this.glNum = glNum;
        }
    }
}

