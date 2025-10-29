package com.example.moneymarket.service;

import com.example.moneymarket.entity.TranTable;
import com.example.moneymarket.repository.ParameterTableRepository;
import com.example.moneymarket.repository.TranTableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for EOD pre-validation checks
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EODValidationService {

    private final ParameterTableRepository parameterTableRepository;
    private final TranTableRepository tranTableRepository;

    @Value("${eod.admin.user:ADMIN}")
    private String eodAdminUser;

    /**
     * Perform all pre-EOD validations
     */
    @Transactional(readOnly = true)
    public EODValidationResult performPreEODValidations(String userId, LocalDate systemDate) {
        log.info("Starting pre-EOD validations for user: {}, system date: {}", userId, systemDate);
        
        // Validation 1: Verify only EOD admin user is logged in
        EODValidationResult adminValidation = validateEODAdminUser(userId);
        if (!adminValidation.isValid()) {
            return adminValidation;
        }
        
        // Validation 2: Verify no transactions remain in 'Entry' status
        EODValidationResult entryStatusValidation = validateNoEntryStatusTransactions(systemDate);
        if (!entryStatusValidation.isValid()) {
            return entryStatusValidation;
        }
        
        // Validation 3: Verify debit-credit balance within Tran_Table
        EODValidationResult balanceValidation = validateDebitCreditBalance(systemDate);
        if (!balanceValidation.isValid()) {
            return balanceValidation;
        }
        
        log.info("All pre-EOD validations passed successfully");
        return EODValidationResult.success("All pre-EOD validations passed");
    }

    /**
     * Validation 1: Verify only the EOD admin user is logged in
     */
    private EODValidationResult validateEODAdminUser(String userId) {
        log.info("Validating EOD admin user: {}", userId);
        
        Optional<String> adminUser = parameterTableRepository.getEODAdminUser();
        String expectedAdminUser = adminUser.orElse(eodAdminUser);
        
        if (!expectedAdminUser.equals(userId)) {
            String errorMessage = String.format("User '%s' is not authorized to run EOD. Only '%s' is allowed.", 
                    userId, expectedAdminUser);
            log.error(errorMessage);
            return EODValidationResult.failure(errorMessage);
        }
        
        log.info("EOD admin user validation passed");
        return EODValidationResult.success("EOD admin user validation passed");
    }

    /**
     * Validation 2: Verify no transactions remain in status 'Entry' - all must be 'Verified'
     */
    private EODValidationResult validateNoEntryStatusTransactions(LocalDate systemDate) {
        log.info("Validating no transactions in 'Entry' status for system date: {}", systemDate);
        
        List<TranTable> entryStatusTransactions = tranTableRepository.findByTranDateBetween(systemDate, systemDate)
                .stream()
                .filter(t -> t.getTranStatus() == TranTable.TranStatus.Entry)
                .collect(Collectors.toList());
        
        if (!entryStatusTransactions.isEmpty()) {
            List<String> tranIds = entryStatusTransactions.stream()
                    .map(TranTable::getTranId)
                    .collect(Collectors.toList());
            
            String errorMessage = String.format("Found %d transactions in 'Entry' status that must be verified before EOD. " +
                    "Transaction IDs: %s", entryStatusTransactions.size(), String.join(", ", tranIds));
            log.error(errorMessage);
            return EODValidationResult.failure(errorMessage);
        }
        
        log.info("No transactions in 'Entry' status found - validation passed");
        return EODValidationResult.success("No transactions in 'Entry' status found");
    }

    /**
     * Validation 3: Verify debit-credit balance within Tran_Table for System_Date
     */
    private EODValidationResult validateDebitCreditBalance(LocalDate systemDate) {
        log.info("Validating debit-credit balance for system date: {}", systemDate);
        
        List<TranTable> transactions = tranTableRepository.findByTranDateBetween(systemDate, systemDate)
                .stream()
                .filter(t -> t.getTranStatus() == TranTable.TranStatus.Verified)
                .collect(Collectors.toList());
        
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        
        for (TranTable transaction : transactions) {
            if (transaction.getDrCrFlag() == TranTable.DrCrFlag.D) {
                totalDebit = totalDebit.add(transaction.getLcyAmt());
            } else if (transaction.getDrCrFlag() == TranTable.DrCrFlag.C) {
                totalCredit = totalCredit.add(transaction.getLcyAmt());
            }
        }
        
        log.info("Total Debit: {}, Total Credit: {}", totalDebit, totalCredit);
        
        if (totalDebit.compareTo(totalCredit) != 0) {
            String errorMessage = String.format("Debit-Credit balance mismatch for system date %s. " +
                    "Total Debit: %s, Total Credit: %s, Difference: %s", 
                    systemDate, totalDebit, totalCredit, totalDebit.subtract(totalCredit));
            log.error(errorMessage);
            return EODValidationResult.failure(errorMessage);
        }
        
        log.info("Debit-credit balance validation passed");
        return EODValidationResult.success("Debit-credit balance validation passed");
    }

    /**
     * EOD Validation Result class
     */
    public static class EODValidationResult {
        private final boolean valid;
        private final String message;

        private EODValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public static EODValidationResult success(String message) {
            return new EODValidationResult(true, message);
        }

        public static EODValidationResult failure(String message) {
            return new EODValidationResult(false, message);
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }

        public String getErrorMessage() {
            return valid ? null : message;
        }
    }
}
