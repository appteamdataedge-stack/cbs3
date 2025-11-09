package com.example.moneymarket.service;

import com.example.moneymarket.entity.ParameterTable;
import com.example.moneymarket.exception.BusinessException;
import com.example.moneymarket.repository.ParameterTableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Service for validating value dates according to business rules
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ValueDateValidationService {

    private final ParameterTableRepository parameterTableRepository;
    private final SystemDateService systemDateService;

    /**
     * Validate value date against business rules:
     * 1. Value date must not be more than pastLimit days before System_Date
     * 2. Value date must not be more than futureLimit days after System_Date
     * 3. Value date must not be before Last EOM date
     * 4. Value date must be a valid business date
     *
     * @param valueDate The value date to validate
     * @throws BusinessException if validation fails
     */
    public void validateValueDate(LocalDate valueDate) {
        LocalDate systemDate = systemDateService.getSystemDate();

        // Get validation parameters from parameter_table
        int pastLimit = getParameterAsInt("Past_Value_Date_Limit_Days", 90);
        int futureLimit = getParameterAsInt("Future_Value_Date_Limit_Days", 30);
        LocalDate lastEOMDate = getParameterAsDate("Last_EOM_Date");

        // Calculate days difference
        long daysInPast = ChronoUnit.DAYS.between(valueDate, systemDate);
        long daysInFuture = ChronoUnit.DAYS.between(systemDate, valueDate);

        // Rule 1: Check past limit
        if (valueDate.isBefore(systemDate) && daysInPast > pastLimit) {
            throw new BusinessException(
                String.format("Value date cannot be more than %d days in the past. " +
                    "Value date: %s, System date: %s, Difference: %d days",
                    pastLimit, valueDate, systemDate, daysInPast)
            );
        }

        // Rule 2: Check future limit
        if (valueDate.isAfter(systemDate) && daysInFuture > futureLimit) {
            throw new BusinessException(
                String.format("Value date cannot be more than %d days in the future. " +
                    "Value date: %s, System date: %s, Difference: %d days",
                    futureLimit, valueDate, systemDate, daysInFuture)
            );
        }

        // Rule 3: Check against last EOM date
        if (lastEOMDate != null && valueDate.isBefore(lastEOMDate)) {
            throw new BusinessException(
                String.format("Value date cannot be before last End of Month date. " +
                    "Value date: %s, Last EOM date: %s",
                    valueDate, lastEOMDate)
            );
        }

        log.debug("Value date validation passed for date: {}", valueDate);
    }

    /**
     * Classify value date relative to system date
     *
     * @param valueDate The value date
     * @return "PAST", "CURRENT", or "FUTURE"
     */
    public String classifyValueDate(LocalDate valueDate) {
        LocalDate systemDate = systemDateService.getSystemDate();

        if (valueDate.isBefore(systemDate)) {
            return "PAST";
        } else if (valueDate.isEqual(systemDate)) {
            return "CURRENT";
        } else {
            return "FUTURE";
        }
    }

    /**
     * Calculate days difference between value date and system date
     * Positive for past-dated, negative for future-dated
     *
     * @param valueDate The value date
     * @return Days difference (positive for past, negative for future)
     */
    public int calculateDaysDifference(LocalDate valueDate) {
        LocalDate systemDate = systemDateService.getSystemDate();
        return (int) ChronoUnit.DAYS.between(valueDate, systemDate);
    }

    /**
     * Get parameter value as integer with default fallback
     */
    private int getParameterAsInt(String parameterName, int defaultValue) {
        return parameterTableRepository.findByParameterName(parameterName)
            .map(ParameterTable::getParameterValue)
            .map(value -> {
                try {
                    return Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    log.warn("Invalid integer value for parameter {}: {}. Using default: {}",
                        parameterName, value, defaultValue);
                    return defaultValue;
                }
            })
            .orElseGet(() -> {
                log.warn("Parameter {} not found. Using default: {}", parameterName, defaultValue);
                return defaultValue;
            });
    }

    /**
     * Get parameter value as LocalDate
     */
    private LocalDate getParameterAsDate(String parameterName) {
        return parameterTableRepository.findByParameterName(parameterName)
            .map(ParameterTable::getParameterValue)
            .map(value -> {
                try {
                    return LocalDate.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                } catch (Exception e) {
                    log.warn("Invalid date value for parameter {}: {}", parameterName, value);
                    return null;
                }
            })
            .orElse(null);
    }
}
