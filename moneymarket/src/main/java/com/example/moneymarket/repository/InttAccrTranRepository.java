package com.example.moneymarket.repository;

import com.example.moneymarket.entity.InttAccrTran;
import com.example.moneymarket.entity.InttAccrTran.AccrualStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InttAccrTranRepository extends JpaRepository<InttAccrTran, String> {
    
    List<InttAccrTran> findByAccountNo(String accountNo);
    
    List<InttAccrTran> findByStatus(AccrualStatus status);
    
    List<InttAccrTran> findByAccrualDate(LocalDate accrualDate);
    
    List<InttAccrTran> findByAccrualDateAndStatus(LocalDate accrualDate, AccrualStatus status);
    
    List<InttAccrTran> findByAccountNoAndAccrualDateBetween(String accountNo, LocalDate startDate, LocalDate endDate);
    
    /**
     * Count interest accrual transactions for a specific date
     * Used for generating sequence numbers in interest accrual ID generation
     *
     * @param tranDate The transaction date
     * @return Count of interest accrual transactions for the date
     */
    long countByTranDate(LocalDate tranDate);

    /**
     * Count interest accrual transactions for a specific accrual date
     */
    long countByAccrualDate(LocalDate accrualDate);

    /**
     * Find the maximum sequential number used for a specific accrual date
     * Extracts the 9-digit sequential part from IDs like S20251020000000001-1
     *
     * @param accrualDate The accrual date to check
     * @return Optional containing the maximum sequential number, or empty if no records exist
     */
    @Query(value = "SELECT MAX(CAST(SUBSTRING(Accr_Tran_Id, 10, 9) AS UNSIGNED)) " +
                   "FROM intt_accr_tran " +
                   "WHERE Accrual_Date = :accrualDate " +
                   "AND Accr_Tran_Id LIKE CONCAT('S', DATE_FORMAT(:accrualDate, '%Y%m%d'), '%')",
           nativeQuery = true)
    Optional<Integer> findMaxSequentialByAccrualDate(@Param("accrualDate") LocalDate accrualDate);

    /**
     * Sum debit amounts for a specific account and accrual date
     * Returns 0 if no records found
     *
     * @param accountNo The account number
     * @param accrualDate The accrual date
     * @return Sum of debit amounts
     */
    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM InttAccrTran i " +
           "WHERE i.accountNo = :accountNo " +
           "AND i.accrualDate = :accrualDate " +
           "AND i.drCrFlag = 'D'")
    BigDecimal sumDebitAmountsByAccountAndDate(@Param("accountNo") String accountNo,
                                                @Param("accrualDate") LocalDate accrualDate);

    /**
     * Sum credit amounts for a specific account and accrual date
     * Returns 0 if no records found
     *
     * @param accountNo The account number
     * @param accrualDate The accrual date
     * @return Sum of credit amounts
     */
    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM InttAccrTran i " +
           "WHERE i.accountNo = :accountNo " +
           "AND i.accrualDate = :accrualDate " +
           "AND i.drCrFlag = 'C'")
    BigDecimal sumCreditAmountsByAccountAndDate(@Param("accountNo") String accountNo,
                                                 @Param("accrualDate") LocalDate accrualDate);

    /**
     * Find distinct account numbers for a specific accrual date
     *
     * @param accrualDate The accrual date
     * @return List of unique account numbers
     */
    @Query("SELECT DISTINCT i.accountNo FROM InttAccrTran i WHERE i.accrualDate = :accrualDate")
    List<String> findDistinctAccountsByAccrualDate(@Param("accrualDate") LocalDate accrualDate);
}
