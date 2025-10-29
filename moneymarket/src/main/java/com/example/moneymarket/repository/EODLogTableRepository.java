package com.example.moneymarket.repository;

import com.example.moneymarket.entity.EODLogTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EODLogTableRepository extends JpaRepository<EODLogTable, Long> {
    
    List<EODLogTable> findByEodDate(LocalDate eodDate);
    
    List<EODLogTable> findByEodDateAndJobName(LocalDate eodDate, String jobName);
    
    List<EODLogTable> findByEodDateOrderByStartTimestamp(LocalDate eodDate);
    
    @Query("SELECT e FROM EODLogTable e WHERE e.eodDate = :date AND e.status = 'Running'")
    List<EODLogTable> findRunningJobsByDate(@Param("date") LocalDate date);
    
    @Query("SELECT e FROM EODLogTable e WHERE e.eodDate = :date AND e.status = 'Failed'")
    List<EODLogTable> findFailedJobsByDate(@Param("date") LocalDate date);
    
    @Query("SELECT e FROM EODLogTable e WHERE e.eodDate = :date AND e.jobName = :jobName ORDER BY e.startTimestamp DESC")
    List<EODLogTable> findLatestJobExecution(@Param("date") LocalDate date, @Param("jobName") String jobName);
    
    Optional<EODLogTable> findTopByEodDateAndJobNameOrderByStartTimestampDesc(LocalDate eodDate, String jobName);
}
