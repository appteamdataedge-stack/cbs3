package com.example.moneymarket.service;

import com.example.moneymarket.entity.TranTable;
import com.example.moneymarket.repository.ParameterTableRepository;
import com.example.moneymarket.repository.TranTableRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EODValidationServiceTest {

    @Mock
    private ParameterTableRepository parameterTableRepository;

    @Mock
    private TranTableRepository tranTableRepository;

    @InjectMocks
    private EODValidationService eodValidationService;

    private LocalDate systemDate;
    private String validUserId;
    private String invalidUserId;

    @BeforeEach
    void setUp() {
        systemDate = LocalDate.of(2024, 1, 15);
        validUserId = "ADMIN";
        invalidUserId = "USER1";
    }

    @Test
    void testValidateEODAdminUser_Success() {
        // Given
        when(parameterTableRepository.getEODAdminUser()).thenReturn(Optional.of("ADMIN"));

        // When
        EODValidationService.EODValidationResult result = eodValidationService.performPreEODValidations(validUserId, systemDate);

        // Then
        assertTrue(result.isValid());
        assertEquals("All pre-EOD validations passed", result.getMessage());
    }

    @Test
    void testValidateEODAdminUser_Failure() {
        // Given
        when(parameterTableRepository.getEODAdminUser()).thenReturn(Optional.of("ADMIN"));

        // When
        EODValidationService.EODValidationResult result = eodValidationService.performPreEODValidations(invalidUserId, systemDate);

        // Then
        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("not authorized to run EOD"));
    }

    @Test
    void testValidateNoEntryStatusTransactions_Success() {
        // Given
        when(parameterTableRepository.getEODAdminUser()).thenReturn(Optional.of("ADMIN"));
        
        TranTable debitTransaction = createTransaction("TXN001", TranTable.TranStatus.Verified, TranTable.DrCrFlag.D, new BigDecimal("1000.00"));
        TranTable creditTransaction = createTransaction("TXN002", TranTable.TranStatus.Verified, TranTable.DrCrFlag.C, new BigDecimal("1000.00"));
        when(tranTableRepository.findByTranDateBetween(any(), any())).thenReturn(Arrays.asList(debitTransaction, creditTransaction));

        // When
        EODValidationService.EODValidationResult result = eodValidationService.performPreEODValidations(validUserId, systemDate);

        // Then
        assertTrue(result.isValid());
        assertEquals("All pre-EOD validations passed", result.getMessage());
    }

    @Test
    void testValidateNoEntryStatusTransactions_Failure() {
        // Given
        when(parameterTableRepository.getEODAdminUser()).thenReturn(Optional.of("ADMIN"));
        
        TranTable entryTransaction = createTransaction("TXN001", TranTable.TranStatus.Entry, TranTable.DrCrFlag.D, new BigDecimal("1000.00"));
        when(tranTableRepository.findByTranDateBetween(any(), any())).thenReturn(Arrays.asList(entryTransaction));

        // When
        EODValidationService.EODValidationResult result = eodValidationService.performPreEODValidations(validUserId, systemDate);

        // Then
        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("transactions in 'Entry' status"));
    }

    @Test
    void testValidateDebitCreditBalance_Success() {
        // Given
        when(parameterTableRepository.getEODAdminUser()).thenReturn(Optional.of("ADMIN"));
        
        TranTable debitTransaction = createTransaction("TXN001", TranTable.TranStatus.Verified, TranTable.DrCrFlag.D, new BigDecimal("1000.00"));
        TranTable creditTransaction = createTransaction("TXN002", TranTable.TranStatus.Verified, TranTable.DrCrFlag.C, new BigDecimal("1000.00"));
        when(tranTableRepository.findByTranDateBetween(any(), any())).thenReturn(Arrays.asList(debitTransaction, creditTransaction));

        // When
        EODValidationService.EODValidationResult result = eodValidationService.performPreEODValidations(validUserId, systemDate);

        // Then
        assertTrue(result.isValid());
    }

    @Test
    void testValidateDebitCreditBalance_Failure() {
        // Given
        when(parameterTableRepository.getEODAdminUser()).thenReturn(Optional.of("ADMIN"));
        
        TranTable debitTransaction = createTransaction("TXN001", TranTable.TranStatus.Verified, TranTable.DrCrFlag.D, new BigDecimal("1000.00"));
        TranTable creditTransaction = createTransaction("TXN002", TranTable.TranStatus.Verified, TranTable.DrCrFlag.C, new BigDecimal("500.00"));
        when(tranTableRepository.findByTranDateBetween(any(), any())).thenReturn(Arrays.asList(debitTransaction, creditTransaction));

        // When
        EODValidationService.EODValidationResult result = eodValidationService.performPreEODValidations(validUserId, systemDate);

        // Then
        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("Debit-Credit balance mismatch"));
    }

    @Test
    void testPerformPreEODValidations_AllValidationsPass() {
        // Given
        when(parameterTableRepository.getEODAdminUser()).thenReturn(Optional.of("ADMIN"));
        
        TranTable debitTransaction = createTransaction("TXN001", TranTable.TranStatus.Verified, TranTable.DrCrFlag.D, new BigDecimal("1000.00"));
        TranTable creditTransaction = createTransaction("TXN002", TranTable.TranStatus.Verified, TranTable.DrCrFlag.C, new BigDecimal("1000.00"));
        when(tranTableRepository.findByTranDateBetween(any(), any())).thenReturn(Arrays.asList(debitTransaction, creditTransaction));

        // When
        EODValidationService.EODValidationResult result = eodValidationService.performPreEODValidations(validUserId, systemDate);

        // Then
        assertTrue(result.isValid());
        assertEquals("All pre-EOD validations passed", result.getMessage());
        
        // Verify all repository methods were called
        verify(parameterTableRepository).getEODAdminUser();
        verify(tranTableRepository, times(2)).findByTranDateBetween(systemDate, systemDate);
    }

    private TranTable createTransaction(String tranId, TranTable.TranStatus status, TranTable.DrCrFlag drCrFlag, BigDecimal amount) {
        return TranTable.builder()
                .tranId(tranId)
                .tranDate(systemDate)
                .valueDate(systemDate)
                .tranStatus(status)
                .drCrFlag(drCrFlag)
                .accountNo("ACC001")
                .tranCcy("BDT")
                .fcyAmt(amount)
                .exchangeRate(new BigDecimal("1.0000"))
                .lcyAmt(amount)
                .build();
    }
}
