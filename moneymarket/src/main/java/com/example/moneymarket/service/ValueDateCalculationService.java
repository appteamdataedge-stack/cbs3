package com.example.moneymarket.service;

import com.example.moneymarket.entity.CustAcctMaster;
import com.example.moneymarket.entity.OFAcctMaster;
import com.example.moneymarket.entity.ParameterTable;
import com.example.moneymarket.entity.SubProdMaster;
import com.example.moneymarket.exception.ResourceNotFoundException;
import com.example.moneymarket.repository.CustAcctMasterRepository;
import com.example.moneymarket.repository.OFAcctMasterRepository;
import com.example.moneymarket.repository.ParameterTableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Service for calculating delta interest for value-dated transactions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ValueDateCalculationService {

    private final CustAcctMasterRepository custAcctMasterRepository;
    private final OFAcctMasterRepository ofAcctMasterRepository;
    private final ParameterTableRepository parameterTableRepository;
    private final UnifiedAccountService unifiedAccountService;

    /**
     * Calculate delta interest for a past-dated transaction
     * Formula: Δ_Interest = Principal × Interest_Rate × Days_Difference / Divisor
     *
     * @param accountNo The account number
     * @param amount The transaction amount
     * @param daysDifference The days difference between value date and system date
     * @return The delta interest amount
     */
    public BigDecimal calculateDeltaInterest(String accountNo, BigDecimal amount, int daysDifference) {
        // For future-dated transactions, no delta interest
        if (daysDifference < 0) {
            return BigDecimal.ZERO;
        }

        // Get account interest rate
        BigDecimal interestRate = getAccountInterestRate(accountNo);

        // If no interest rate configured, return zero
        if (interestRate == null || interestRate.compareTo(BigDecimal.ZERO) == 0) {
            log.debug("No interest rate for account {}. Delta interest = 0", accountNo);
            return BigDecimal.ZERO;
        }

        // Get divisor from parameter table (default: 36500)
        BigDecimal divisor = getInterestDivisor();

        // Calculate delta interest: Principal × Rate × Days / Divisor
        BigDecimal deltaInterest = amount.abs()
            .multiply(interestRate)
            .multiply(new BigDecimal(daysDifference))
            .divide(divisor, 4, RoundingMode.HALF_UP);

        log.debug("Delta interest calculation for account {}: Amount={}, Rate={}, Days={}, Divisor={}, Delta={}",
            accountNo, amount, interestRate, daysDifference, divisor, deltaInterest);

        return deltaInterest;
    }

    /**
     * Get the interest rate for an account
     * Checks both customer and office accounts, and falls back to sub-product rate
     *
     * @param accountNo The account number
     * @return The interest rate (as decimal, e.g., 0.05 for 5%)
     */
    private BigDecimal getAccountInterestRate(String accountNo) {
        // Try customer account first
        try {
            CustAcctMaster custAccount = custAcctMasterRepository.findById(accountNo)
                .orElseThrow(() -> new ResourceNotFoundException("Customer Account", "Account Number", accountNo));

            SubProdMaster subProduct = custAccount.getSubProduct();
            if (subProduct != null && subProduct.getEffectiveInterestRate() != null) {
                // Interest rate stored as percentage (e.g., 5.0 for 5%)
                // Convert to decimal (0.05 for 5%)
                return subProduct.getEffectiveInterestRate().divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
            }

            log.warn("No interest rate found for customer account {} sub-product", accountNo);
            return BigDecimal.ZERO;

        } catch (ResourceNotFoundException e) {
            // Try office account
            try {
                OFAcctMaster officeAccount = ofAcctMasterRepository.findById(accountNo)
                    .orElseThrow(() -> new ResourceNotFoundException("Office Account", "Account Number", accountNo));

                SubProdMaster subProduct = officeAccount.getSubProduct();
                if (subProduct != null && subProduct.getEffectiveInterestRate() != null) {
                    // Convert percentage to decimal
                    return subProduct.getEffectiveInterestRate().divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
                }

                log.warn("No interest rate found for office account {} sub-product", accountNo);
                return BigDecimal.ZERO;

            } catch (ResourceNotFoundException ex) {
                log.error("Account {} not found in customer or office accounts", accountNo);
                throw ex;
            }
        }
    }

    /**
     * Get the interest divisor from parameter table
     * Default: 36500 (365 days × 100 for percentage-based calculation)
     *
     * @return The interest divisor
     */
    private BigDecimal getInterestDivisor() {
        return parameterTableRepository.findByParameterName("Interest_Default_Divisor")
            .map(ParameterTable::getParameterValue)
            .map(value -> {
                try {
                    return new BigDecimal(value);
                } catch (NumberFormatException e) {
                    log.warn("Invalid divisor value in parameter table: {}. Using default 36500", value);
                    return new BigDecimal("36500");
                }
            })
            .orElseGet(() -> {
                log.warn("Interest_Default_Divisor not found in parameter table. Using default 36500");
                return new BigDecimal("36500");
            });
    }
}
