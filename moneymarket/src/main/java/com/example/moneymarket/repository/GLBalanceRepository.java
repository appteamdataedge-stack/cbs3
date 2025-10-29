package com.example.moneymarket.repository;

import com.example.moneymarket.entity.GLBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for GL Balance operations
 *
 * SCHEMA CHANGE (2025-10-23):
 * - Changed from composite PK (GLBalance.GLBalanceId) to Long id
 * - This simplifies repository interface and queries
 */
@Repository
public interface GLBalanceRepository extends JpaRepository<GLBalance, Long> {

    /**
     * Find GL balance by GL number with pessimistic write lock
     * Note: This will return the most recent record if multiple exist for the same GL
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT gb FROM GLBalance gb WHERE gb.glNum = ?1 ORDER BY gb.tranDate DESC")
    Optional<GLBalance> findByGlNumWithLock(String glNum);

    @Query(value = "SELECT * FROM gl_balance WHERE GL_Num = ?1 AND Tran_date = ?2 LIMIT 1",
           nativeQuery = true)
    Optional<GLBalance> findByGlNumAndTranDate(String glNum, LocalDate tranDate);

    @Query(value = "SELECT gb FROM GLBalance gb WHERE gb.glNum = ?1 AND gb.tranDate = ?2",
           nativeQuery = false)
    Optional<GLBalance> findByGlSetupGlNumAndTranDate(String glNum, LocalDate tranDate);

    List<GLBalance> findByTranDate(LocalDate tranDate);

    /**
     * Find the latest balance record for a GL number
     * @param glNum The GL number
     * @return Optional containing the latest balance record
     */
    @Query(value = "SELECT * FROM gl_balance WHERE GL_Num = ?1 ORDER BY Tran_date DESC LIMIT 1", nativeQuery = true)
    Optional<GLBalance> findLatestByGlNum(String glNum);

    /**
     * Get the closing balance for a specific GL and date (returns scalar value only)
     * @param glNum The GL number
     * @param tranDate The transaction date
     * @return The closing balance as BigDecimal
     */
    @Query(value = "SELECT Closing_Bal FROM gl_balance WHERE GL_Num = ?1 AND Tran_date = ?2 LIMIT 1",
           nativeQuery = true)
    BigDecimal findClosingBalByGlNumAndTranDate(String glNum, LocalDate tranDate);

    /**
     * Find all GL balance records for a GL number before a specific date
     * Ordered by transaction date descending (most recent first)
     * Used for 3-tier fallback logic to find opening balance
     *
     * @param glNum The GL number
     * @param tranDate The transaction date (exclusive)
     * @return List of GL balance records ordered by date descending
     */
    @Query("SELECT gb FROM GLBalance gb WHERE gb.glNum = ?1 AND gb.tranDate < ?2 ORDER BY gb.tranDate DESC")
    List<GLBalance> findByGlNumAndTranDateBeforeOrderByTranDateDesc(String glNum, LocalDate tranDate);

    /**
     * Find GL balances by transaction date, filtered to only include GL numbers that are actively used
     * This is used for financial reports to exclude unused GLs
     *
     * @param tranDate The transaction date
     * @param glNumbers List of active GL numbers to include
     * @return List of GL balances for the specified date and GL numbers
     */
    @Query("SELECT gb FROM GLBalance gb WHERE gb.tranDate = ?1 AND gb.glNum IN ?2")
    List<GLBalance> findByTranDateAndGlNumIn(LocalDate tranDate, List<String> glNumbers);
}
