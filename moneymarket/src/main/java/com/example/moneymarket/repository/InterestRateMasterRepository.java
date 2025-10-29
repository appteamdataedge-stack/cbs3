package com.example.moneymarket.repository;

import com.example.moneymarket.entity.InterestRateMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;

public interface InterestRateMasterRepository extends JpaRepository<InterestRateMaster, String> {
    Optional<InterestRateMaster> findTopByInttCodeAndInttEffctvDateLessThanEqualOrderByInttEffctvDateDesc(String inttCode, LocalDate asOfDate);
}


