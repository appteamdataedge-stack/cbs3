package com.example.moneymarket.repository;

import com.example.moneymarket.entity.SubProdMaster;
import com.example.moneymarket.entity.SubProdMaster.SubProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubProdMasterRepository extends JpaRepository<SubProdMaster, Integer> {
    
    boolean existsBySubProductCode(String subProductCode);
    
    Optional<SubProdMaster> findBySubProductCode(String subProductCode);
    
    List<SubProdMaster> findByProductProductId(Integer productId);
    
    List<SubProdMaster> findBySubProductStatus(SubProductStatus status);
    
    Optional<SubProdMaster> findByCumGLNum(String cumGLNum);
    
    /**
     * Find SubProduct by ID with Product relationship loaded
     * 
     * @param subProductId The sub-product ID
     * @return Optional SubProduct with Product loaded
     */
    @Query("SELECT sp FROM SubProdMaster sp JOIN FETCH sp.product WHERE sp.subProductId = :subProductId")
    Optional<SubProdMaster> findByIdWithProduct(@Param("subProductId") Integer subProductId);
}
