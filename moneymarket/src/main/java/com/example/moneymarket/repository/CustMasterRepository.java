package com.example.moneymarket.repository;

import com.example.moneymarket.entity.CustMaster;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustMasterRepository extends JpaRepository<CustMaster, Integer> {
    
    boolean existsByExtCustId(String extCustId);
    
    Optional<CustMaster> findByExtCustId(String extCustId);
    
    /**
     * Find the maximum customer ID that starts with the given prefix
     * 
     * @param prefix The prefix to match (e.g., "1%", "2%", "3%")
     * @return The maximum customer ID with the given prefix, or null if none found
     */
    @Query(value = "SELECT MAX(c.custId) FROM CustMaster c WHERE CAST(c.custId AS string) LIKE :prefix")
    Integer findMaxCustIdWithPrefix(@Param("prefix") String prefix);
    
    /**
     * Search customers by various fields
     * 
     * @param searchTerm The search term to match
     * @param pageable The pagination information
     * @return Page of customers matching the search criteria
     */
    @Query("SELECT c FROM CustMaster c WHERE " +
           "CAST(c.custId AS string) LIKE %:searchTerm% OR " +
           "c.extCustId LIKE %:searchTerm% OR " +
           "c.firstName LIKE %:searchTerm% OR " +
           "c.lastName LIKE %:searchTerm% OR " +
           "c.tradeName LIKE %:searchTerm% OR " +
           "c.mobile LIKE %:searchTerm%")
    Page<CustMaster> searchCustomers(@Param("searchTerm") String searchTerm, Pageable pageable);
}
