package com.example.moneymarket.repository;

import com.example.moneymarket.entity.OFAcctMaster;
import com.example.moneymarket.entity.OFAcctMaster.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OFAcctMasterRepository extends JpaRepository<OFAcctMaster, String> {
    
    List<OFAcctMaster> findBySubProductSubProductId(Integer subProductId);
    
    List<OFAcctMaster> findByGlNum(String glNum);
    
    List<OFAcctMaster> findByAccountStatus(AccountStatus status);
    
    List<OFAcctMaster> findByReconciliationRequired(Boolean reconciliationRequired);

    List<OFAcctMaster> findByGlNumAndAccountStatus(String glNum, AccountStatus status);

    boolean existsByAccountNo(String accountNo);
}
