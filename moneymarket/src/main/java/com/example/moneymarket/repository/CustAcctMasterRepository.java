package com.example.moneymarket.repository;

import com.example.moneymarket.entity.CustAcctMaster;
import com.example.moneymarket.entity.CustAcctMaster.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustAcctMasterRepository extends JpaRepository<CustAcctMaster, String> {
    
    List<CustAcctMaster> findByCustomerCustId(Integer custId);
    
    List<CustAcctMaster> findBySubProductSubProductId(Integer subProductId);
    
    List<CustAcctMaster> findByGlNum(String glNum);
    
    List<CustAcctMaster> findByAccountStatus(AccountStatus status);
    
    boolean existsByAccountNo(String accountNo);
    
    /**
     * Find the maximum sequence number for a given customer and product type
     * The sequence is the last 3 digits of the account number
     * 
     * @param custId The customer ID
     * @param productType The product type code (9th digit of account number)
     * @return The maximum sequence number, or null if none found
     */
    @Query(value = "SELECT MAX(CAST(SUBSTRING(c.accountNo, 10, 3) AS int)) " +
           "FROM CustAcctMaster c " +
           "WHERE c.customer.custId = :custId " +
           "AND SUBSTRING(c.accountNo, 9, 1) = :productType")
    Integer findMaxSequenceForCustomerAndProductType(
            @Param("custId") Integer custId, 
            @Param("productType") String productType);
}
