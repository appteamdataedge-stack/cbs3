package com.example.moneymarket.repository;

import com.example.moneymarket.entity.ProdMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProdMasterRepository extends JpaRepository<ProdMaster, Integer> {
    
    boolean existsByProductCode(String productCode);
    
    Optional<ProdMaster> findByProductCode(String productCode);
    
    Optional<ProdMaster> findByCumGLNum(String cumGLNum);
}
