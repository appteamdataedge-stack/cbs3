package com.example.moneymarket.repository;

import com.example.moneymarket.entity.ParameterTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ParameterTableRepository extends JpaRepository<ParameterTable, Integer> {
    
    Optional<ParameterTable> findByParameterName(String parameterName);
    
    @Query("SELECT p FROM ParameterTable p WHERE p.parameterName = :name")
    Optional<ParameterTable> findParameterByName(@Param("name") String name);
    
    @Query("SELECT p.parameterValue FROM ParameterTable p WHERE p.parameterName = 'System_Date'")
    Optional<String> getSystemDate();
    
    @Query("SELECT p.parameterValue FROM ParameterTable p WHERE p.parameterName = 'Last_EOD_Date'")
    Optional<String> getLastEODDate();
    
    @Query("SELECT p.parameterValue FROM ParameterTable p WHERE p.parameterName = 'EOD_Admin_User'")
    Optional<String> getEODAdminUser();
    
    /**
     * Update the System_Date parameter in Parameter_Table
     * 
     * @param systemDate The new system date value
     * @param userId The user ID making the change
     * @param lastUpdated The timestamp of the update
     */
    @Modifying
    @Transactional
    @Query("UPDATE ParameterTable p SET p.parameterValue = :systemDate, p.updatedBy = :userId, p.lastUpdated = :lastUpdated WHERE p.parameterName = 'System_Date'")
    void updateSystemDate(@Param("systemDate") String systemDate, @Param("userId") String userId, @Param("lastUpdated") LocalDateTime lastUpdated);
}
