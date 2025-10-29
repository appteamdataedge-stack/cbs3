package com.example.moneymarket.service;

import com.example.moneymarket.exception.SystemDateNotConfiguredException;
import com.example.moneymarket.repository.ParameterTableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

/**
 * Service to manage system date for the application
 * CBS Compliance: All dates must originate from Parameter_Table System_Date
 * This ensures reproducible EOD operations and audit integrity
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SystemDateService {

    private final ParameterTableRepository parameterTableRepository;
    
    @Value("${system.date:}")
    private String configuredSystemDate;

    /**
     * Get the current system date from Parameter_Table
     * CBS Compliance: Never uses device clock - always uses configured System_Date
     * 
     * @return The system date from Parameter_Table
     * @throws SystemDateNotConfiguredException if System_Date is not configured
     */
    public LocalDate getSystemDate() {
        // First try to get from Parameter_Table
        Optional<String> systemDateStr = parameterTableRepository.getSystemDate();
        if (systemDateStr.isPresent() && !systemDateStr.get().trim().isEmpty()) {
            try {
                LocalDate systemDate = LocalDate.parse(systemDateStr.get());
                log.debug("Retrieved System_Date from Parameter_Table: {}", systemDate);
                return systemDate;
            } catch (Exception e) {
                log.error("Invalid System_Date format in Parameter_Table: {}", systemDateStr.get(), e);
                throw new SystemDateNotConfiguredException(
                    "Invalid System_Date format in Parameter_Table: " + systemDateStr.get(), e);
            }
        }
        
        // Fallback to application configuration
        if (configuredSystemDate != null && !configuredSystemDate.trim().isEmpty()) {
            try {
                LocalDate systemDate = LocalDate.parse(configuredSystemDate);
                log.info("Using System_Date from application configuration: {}", systemDate);
                return systemDate;
            } catch (Exception e) {
                log.error("Invalid System_Date format in application configuration: {}", configuredSystemDate, e);
                throw new SystemDateNotConfiguredException(
                    "Invalid System_Date format in application configuration: " + configuredSystemDate, e);
            }
        }
        
        // CBS Compliance: Never fallback to device clock
        throw new SystemDateNotConfiguredException(
            "System_Date is not configured. Please set System_Date in Parameter_Table or application configuration.");
    }

    /**
     * Get the current system date-time
     * CBS Compliance: Uses System_Date + current system time for timestamps
     * 
     * @return The system date-time (System_Date + current time)
     * @throws SystemDateNotConfiguredException if System_Date is not configured
     */
    public LocalDateTime getSystemDateTime() {
        LocalDate systemDate = getSystemDate();
        LocalTime currentTime = LocalTime.now(); // Use current time for timestamps
        LocalDateTime systemDateTime = LocalDateTime.of(systemDate, currentTime);
        log.debug("Generated System_DateTime: {}", systemDateTime);
        return systemDateTime;
    }

    /**
     * Set the system date in Parameter_Table
     * 
     * @param date The date to set as system date
     * @param userId The user ID making the change
     */
    @Transactional
    public void setSystemDate(LocalDate date, String userId) {
        try {
            // Update the System_Date parameter in Parameter_Table
            parameterTableRepository.updateSystemDate(date.toString(), userId, LocalDateTime.now());
            log.info("System date successfully updated to: {} by user: {}", date, userId);
        } catch (Exception e) {
            log.error("Failed to update System_Date in Parameter_Table: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update System_Date: " + e.getMessage(), e);
        }
    }

    /**
     * Check if System_Date is properly configured
     * 
     * @return true if System_Date is configured, false otherwise
     */
    public boolean isSystemDateConfigured() {
        try {
            getSystemDate();
            return true;
        } catch (SystemDateNotConfiguredException e) {
            return false;
        }
    }

    /**
     * Get System_Date as string for logging/debugging
     * 
     * @return System_Date as string or "NOT_CONFIGURED"
     */
    public String getSystemDateString() {
        try {
            return getSystemDate().toString();
        } catch (SystemDateNotConfiguredException e) {
            return "NOT_CONFIGURED";
        }
    }
}
