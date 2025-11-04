package com.example.moneymarket.repository;

import com.example.moneymarket.entity.TxnHistAcct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for TxnHistAcct entity
 * Provides data access methods for Statement of Accounts functionality
 */
@Repository
public interface TxnHistAcctRepository extends JpaRepository<TxnHistAcct, Long> {

    /**
     * Find all transactions for an account within a date range, ordered chronologically
     * Used for generating Statement of Accounts
     *
     * @param accNo Account number
     * @param fromDate Start date (inclusive)
     * @param toDate End date (inclusive)
     * @return List of transactions ordered by transaction date and creation time
     */
    List<TxnHistAcct> findByAccNoAndTranDateBetweenOrderByTranDateAscRcreTimeAsc(
            String accNo, LocalDate fromDate, LocalDate toDate);

    /**
     * Find the last transaction before a specific date for an account
     * Used to determine opening balance for Statement of Accounts
     *
     * @param accNo Account number
     * @param beforeDate Date before which to find the last transaction
     * @return Optional containing the last transaction if found
     */
    @Query(value = "SELECT * FROM txn_hist_acct WHERE ACC_No = :accNo AND TRAN_DATE < :beforeDate " +
                   "ORDER BY TRAN_DATE DESC, RCRE_TIME DESC LIMIT 1", nativeQuery = true)
    Optional<TxnHistAcct> findLastTransactionBeforeDate(
            @Param("accNo") String accNo, @Param("beforeDate") LocalDate beforeDate);

    /**
     * Count transactions for an account within a date range
     * Used to check transaction volume before generating SOA
     *
     * @param accNo Account number
     * @param fromDate Start date (inclusive)
     * @param toDate End date (inclusive)
     * @return Count of transactions
     */
    Long countByAccNoAndTranDateBetween(String accNo, LocalDate fromDate, LocalDate toDate);

    /**
     * Find the most recent balance for an account
     * Used to get current balance from transaction history
     *
     * @param accNo Account number
     * @return Optional containing the latest balance if found
     */
    @Query(value = "SELECT BALANCE_AFTER_TRAN FROM txn_hist_acct WHERE ACC_No = :accNo " +
                   "ORDER BY TRAN_DATE DESC, RCRE_TIME DESC LIMIT 1", nativeQuery = true)
    Optional<BigDecimal> findLatestBalanceForAccount(@Param("accNo") String accNo);

    /**
     * Find the maximum transaction serial number for a specific transaction ID
     * Used to determine the next serial number for multi-leg transactions
     *
     * @param tranId Transaction ID
     * @return Maximum serial number, or null if no records exist
     */
    @Query("SELECT MAX(t.tranSlNo) FROM TxnHistAcct t WHERE t.tranId = :tranId")
    Integer findMaxTranSlNoByTranId(@Param("tranId") String tranId);

    /**
     * Find all transactions for a specific transaction ID
     * Used to retrieve all legs of a multi-leg transaction
     *
     * @param tranId Transaction ID
     * @return List of transaction history records
     */
    List<TxnHistAcct> findByTranIdOrderByTranSlNoAsc(String tranId);

    /**
     * Find all transactions for an account on a specific date
     * Used for balance calculations and reconciliation
     *
     * @param accNo Account number
     * @param tranDate Transaction date
     * @return List of transactions for the specified date
     */
    List<TxnHistAcct> findByAccNoAndTranDateOrderByRcreTimeAsc(String accNo, LocalDate tranDate);
}

