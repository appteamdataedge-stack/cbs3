package com.example.moneymarket.repository;

import com.example.moneymarket.entity.GLMovementAccrual;
import com.example.moneymarket.entity.InttAccrTran.AccrualStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface GLMovementAccrualRepository extends JpaRepository<GLMovementAccrual, Long> {

    List<GLMovementAccrual> findByAccrualAccrTranId(String accrTranId);

    List<GLMovementAccrual> findByGlSetupGlNum(String glNum);

    List<GLMovementAccrual> findByStatus(AccrualStatus status);

    List<GLMovementAccrual> findByAccrualDate(LocalDate accrualDate);

    List<GLMovementAccrual> findByGlSetupGlNumAndAccrualDateBetween(String glNum, LocalDate startDate, LocalDate endDate);

    /**
     * Check if a GL movement accrual record already exists for a given Accr_Tran_Id
     * Used to prevent duplicate GL movements in Batch Job 3
     *
     * @param accrTranId The accrual transaction ID to check
     * @return true if record exists, false otherwise
     */
    boolean existsByAccrualAccrTranId(String accrTranId);

    /**
     * Count GL movement accrual records for a given Accr_Tran_Id
     * Alternative method for checking duplicates
     *
     * @param accrTranId The accrual transaction ID to check
     * @return Count of records (should be 0 or 1 due to unique constraint)
     */
    long countByAccrualAccrTranId(String accrTranId);

    /**
     * FIX: Get unique GL numbers from GL movement accrual records for a given date using native query
     * CHANGED: Replaced JPQL with native SQL to prevent GLSetup join issues with LAZY fetch
     *
     * @param accrualDate The accrual date
     * @return List of unique GL numbers
     */
    @Query(value = "SELECT DISTINCT GL_Num FROM gl_movement_accrual WHERE Accrual_Date = :accrualDate", nativeQuery = true)
    List<String> findDistinctGLNumbersByAccrualDate(@Param("accrualDate") LocalDate accrualDate);

    /**
     * FIX: Native query to calculate DR/CR summation from accrual table without joining GLSetup
     * This prevents Hibernate duplicate-row assertion errors when GL_Num is not unique
     * 
     * Returns: [GL_Num, Total_DR_Amount, Total_CR_Amount]
     * 
     * @param glNum The GL account number
     * @param fromDate Start date (inclusive)
     * @param toDate End date (inclusive)
     * @return Object array containing [glNum, totalDr, totalCr]
     */
    @Transactional(readOnly = true)
    @Query(value = """
        SELECT 
            GL_Num,
            COALESCE(SUM(CASE WHEN Dr_Cr_Flag = 'D' THEN Amount ELSE 0 END), 0) AS totalDr,
            COALESCE(SUM(CASE WHEN Dr_Cr_Flag = 'C' THEN Amount ELSE 0 END), 0) AS totalCr
        FROM gl_movement_accrual
        WHERE GL_Num = :glNum
          AND Accrual_Date BETWEEN :fromDate AND :toDate
        GROUP BY GL_Num
        """, nativeQuery = true)
    List<Object[]> findDrCrSummationNative(@Param("glNum") String glNum,
                                           @Param("fromDate") LocalDate fromDate,
                                           @Param("toDate") LocalDate toDate);
}
