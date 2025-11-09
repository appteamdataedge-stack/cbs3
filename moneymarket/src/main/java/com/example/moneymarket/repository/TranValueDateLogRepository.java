package com.example.moneymarket.repository;

import com.example.moneymarket.entity.TranValueDateLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TranValueDateLogRepository extends JpaRepository<TranValueDateLog, Long> {

    /**
     * Find log entry by transaction ID
     */
    Optional<TranValueDateLog> findByTranId(String tranId);

    /**
     * Find all future-dated transactions that are not yet posted
     */
    @Query("SELECT v FROM TranValueDateLog v WHERE v.adjustmentPostedFlag = 'N' AND v.valueDate <= :systemDate")
    List<TranValueDateLog> findUnpostedFutureDatedTransactions(@Param("systemDate") LocalDate systemDate);

    /**
     * Update adjustment posted flag for a transaction
     */
    @Modifying
    @Query("UPDATE TranValueDateLog v SET v.adjustmentPostedFlag = :flag WHERE v.tranId = :tranId")
    void updateAdjustmentPostedFlag(@Param("tranId") String tranId, @Param("flag") String flag);

    /**
     * Find all value-dated transactions for a specific date range
     */
    @Query("SELECT v FROM TranValueDateLog v WHERE v.valueDate BETWEEN :startDate AND :endDate")
    List<TranValueDateLog> findByValueDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
