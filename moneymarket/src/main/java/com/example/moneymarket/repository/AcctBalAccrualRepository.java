package com.example.moneymarket.repository;

import com.example.moneymarket.entity.AcctBalAccrual;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AcctBalAccrualRepository extends JpaRepository<AcctBalAccrual, Long> {

    List<AcctBalAccrual> findByAccountAccountNo(String accountNo);

    List<AcctBalAccrual> findByAccrualDate(LocalDate accrualDate);

    List<AcctBalAccrual> findByAccountAccountNoAndAccrualDateBetween(String accountNo, LocalDate startDate, LocalDate endDate);

    Optional<AcctBalAccrual> findByAccountAccountNoAndTranDate(String accountNo, LocalDate tranDate);

    /**
     * Find all account balance accrual records for an account before a specific date
     * Ordered by transaction date descending (most recent first)
     * Used for 3-tier fallback logic to find opening balance
     *
     * @param accountNo The account number
     * @param tranDate The transaction date (exclusive)
     * @return List of accrual balance records ordered by date descending
     */
    List<AcctBalAccrual> findByAccountAccountNoAndTranDateBeforeOrderByTranDateDesc(String accountNo, LocalDate tranDate);

    /**
     * Find the latest accrual record for an account by querying directly on Account_No column
     * This bypasses the @ManyToOne relationship to avoid join issues
     * Orders by Tran_date descending to get the most recent record
     *
     * @param accountNo The account number
     * @return Optional of the latest accrual record
     */
    @org.springframework.data.jpa.repository.Query(
        value = "SELECT * FROM acct_bal_accrual WHERE Account_No = ?1 AND Tran_date IS NOT NULL ORDER BY Tran_date DESC LIMIT 1",
        nativeQuery = true
    )
    Optional<AcctBalAccrual> findLatestByAccountNo(String accountNo);
}
