package com.example.moneymarket.repository;

import com.example.moneymarket.entity.TranTable;
import com.example.moneymarket.entity.TranTable.TranStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TranTableRepository extends JpaRepository<TranTable, String> {
    
    List<TranTable> findByAccountNo(String accountNo);
    
    List<TranTable> findByTranStatus(TranStatus status);
    
    List<TranTable> findByTranDateBetween(LocalDate startDate, LocalDate endDate);
    
    List<TranTable> findByValueDateBetween(LocalDate startDate, LocalDate endDate);
    
    List<TranTable> findByTranDateAndTranStatus(LocalDate tranDate, TranStatus status);
    
    /**
     * Count transactions for a specific date
     * Used for generating sequence numbers in transaction ID generation
     * 
     * @param tranDate The transaction date
     * @return Count of transactions for the date
     */
    long countByTranDate(LocalDate tranDate);
    
    /**
     * Sum all debit transactions for an account on a specific date
     * 
     * @param accountNo The account number
     * @param tranDate The transaction date
     * @return The sum of debit amounts
     */
    @Query("SELECT COALESCE(SUM(t.debitAmount), 0) FROM TranTable t " +
           "WHERE t.accountNo = :accountNo AND t.tranDate = :tranDate")
    Optional<BigDecimal> sumDebitTransactionsForAccountOnDate(
            @Param("accountNo") String accountNo, 
            @Param("tranDate") LocalDate tranDate);
    
    /**
     * Sum all credit transactions for an account on a specific date
     * 
     * @param accountNo The account number
     * @param tranDate The transaction date
     * @return The sum of credit amounts
     */
    @Query("SELECT COALESCE(SUM(t.creditAmount), 0) FROM TranTable t " +
           "WHERE t.accountNo = :accountNo AND t.tranDate = :tranDate")
    Optional<BigDecimal> sumCreditTransactionsForAccountOnDate(
            @Param("accountNo") String accountNo, 
            @Param("tranDate") LocalDate tranDate);
    
    /**
     * Find transactions by account number and transaction date
     * 
     * @param accountNo The account number
     * @param tranDate The transaction date
     * @return List of transactions
     */
    List<TranTable> findByAccountNoAndTranDate(String accountNo, LocalDate tranDate);
}
