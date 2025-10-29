package com.example.moneymarket.repository;

import com.example.moneymarket.entity.GLMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface GLMovementRepository extends JpaRepository<GLMovement, Long> {
    
    List<GLMovement> findByGlSetupGlNum(String glNum);
    
    List<GLMovement> findByTransactionTranId(String tranId);
    
    List<GLMovement> findByTranDate(LocalDate tranDate);
    
    List<GLMovement> findByValueDate(LocalDate valueDate);
    
    List<GLMovement> findByGlSetupGlNumAndTranDateBetween(String glNum, LocalDate startDate, LocalDate endDate);

    List<GLMovement> findByGlSetupAndTranDate(com.example.moneymarket.entity.GLSetup glSetup, LocalDate tranDate);

    boolean existsByTransactionTranId(String tranId);

    /**
     * FIX: Get unique GL numbers from GL movement records for a given date using native query
     * CHANGED: Replaced JPQL with native SQL to prevent GLSetup join issues with LAZY fetch
     *
     * @param tranDate The transaction date
     * @return List of unique GL numbers
     */
    @Query(value = "SELECT DISTINCT GL_Num FROM GL_Movement WHERE Tran_Date = :tranDate", nativeQuery = true)
    List<String> findDistinctGLNumbersByTranDate(@Param("tranDate") LocalDate tranDate);

    /**
     * FIX: Native query to calculate DR/CR summation without joining GLSetup table
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
        FROM GL_Movement
        WHERE GL_Num = :glNum
          AND Tran_Date BETWEEN :fromDate AND :toDate
        GROUP BY GL_Num
        """, nativeQuery = true)
    List<Object[]> findDrCrSummationNative(@Param("glNum") String glNum,
                                           @Param("fromDate") LocalDate fromDate,
                                           @Param("toDate") LocalDate toDate);
}
