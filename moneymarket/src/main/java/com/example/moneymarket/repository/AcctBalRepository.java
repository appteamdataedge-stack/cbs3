package com.example.moneymarket.repository;

import com.example.moneymarket.entity.AcctBal;
import com.example.moneymarket.entity.AcctBalId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AcctBalRepository extends JpaRepository<AcctBal, AcctBalId> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ab FROM AcctBal ab WHERE ab.accountNo = ?1 AND ab.tranDate = ?2")
    Optional<AcctBal> findByAccountNoAndTranDateWithLock(String accountNo, LocalDate tranDate);
    
    @Query("SELECT ab FROM AcctBal ab WHERE ab.accountNo = ?1 AND ab.tranDate = ?2")
    Optional<AcctBal> findByAccountNoAndTranDate(String accountNo, LocalDate tranDate);
    
    @Query("SELECT ab FROM AcctBal ab WHERE ab.accountNo = ?1 ORDER BY ab.tranDate DESC")
    List<AcctBal> findByAccountNoOrderByTranDateDesc(String accountNo);
    
    @Query("SELECT ab FROM AcctBal ab WHERE ab.accountNo = ?1 AND ab.tranDate < ?2 ORDER BY ab.tranDate DESC")
    List<AcctBal> findByAccountNoAndTranDateBeforeOrderByTranDateDesc(String accountNo, LocalDate tranDate);
    
    @Query("SELECT ab FROM AcctBal ab WHERE ab.tranDate = ?1")
    List<AcctBal> findByTranDate(LocalDate tranDate);
    
    // Get the latest balance record for an account (for current balance)
    default Optional<AcctBal> findLatestByAccountNo(String accountNo) {
        List<AcctBal> balances = findByAccountNoOrderByTranDateDesc(accountNo);
        return balances.isEmpty() ? Optional.empty() : Optional.of(balances.get(0));
    }
}
