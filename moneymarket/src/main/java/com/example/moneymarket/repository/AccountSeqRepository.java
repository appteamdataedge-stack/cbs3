package com.example.moneymarket.repository;

import com.example.moneymarket.entity.AccountSeq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface AccountSeqRepository extends JpaRepository<AccountSeq, String> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AccountSeq a WHERE a.glNum = ?1")
    Optional<AccountSeq> findByGlNumWithLock(String glNum);
}
