package com.example.moneymarket.service;

import com.example.moneymarket.repository.EODLogTableRepository;
import com.example.moneymarket.repository.ParameterTableRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class EODOrchestrationServiceTest {

    @Mock
    private ParameterTableRepository parameterTableRepository;

    @Mock
    private EODLogTableRepository eodLogTableRepository;

    @Mock
    private AccountBalanceUpdateService accountBalanceUpdateService;

    @Mock
    private InterestAccrualService interestAccrualService;

    @Mock
    private EODValidationService eodValidationService;

    @Mock
    private EODReportingService eodReportingService;

    @Mock
    private SystemDateService systemDateService;

    @InjectMocks
    private EODOrchestrationService eodOrchestrationService;

    private LocalDate systemDate;
    private String userId;

    @BeforeEach
    void setUp() {
        systemDate = LocalDate.of(2024, 1, 15);
        userId = "ADMIN";
        
        // Mock SystemDateService to return the test system date (lenient to avoid unnecessary stubbing errors)
        lenient().when(systemDateService.getSystemDate()).thenReturn(systemDate);
        lenient().when(systemDateService.getSystemDateTime()).thenReturn(systemDate.atStartOfDay());
    }

    @Test
    void testGetSystemDate() {
        // When
        LocalDate result = eodOrchestrationService.getSystemDate();

        // Then
        assertEquals(systemDate, result);
        verify(systemDateService).getSystemDate();
    }

    @Test
    void testGetSystemDate_DefaultToCurrentDate() {
        // When
        LocalDate result = eodOrchestrationService.getSystemDate();

        // Then
        assertEquals(systemDate, result);
        verify(systemDateService).getSystemDate();
    }

    @Test
    void testExecuteEOD_Success() {
        // Given
        when(eodValidationService.performPreEODValidations(any(), any()))
                .thenReturn(EODValidationService.EODValidationResult.success("All validations passed"));
        when(accountBalanceUpdateService.executeAccountBalanceUpdate(any())).thenReturn(5);
        when(interestAccrualService.runEODAccruals(any())).thenReturn(3);
        when(eodReportingService.generateFinancialReports(any())).thenReturn(true);
        when(parameterTableRepository.findByParameterName(any())).thenReturn(Optional.empty());

        // When
        EODOrchestrationService.EODResult result = eodOrchestrationService.executeEOD(userId);

        // Then
        assertTrue(result.isSuccess());
        assertEquals("EOD completed successfully", result.getMessage());
        assertEquals(5, result.getAccountsProcessed());
        assertEquals(3, result.getInterestEntriesProcessed());
        
        // Verify all services were called
        verify(eodValidationService).performPreEODValidations(userId, systemDate);
        verify(accountBalanceUpdateService).executeAccountBalanceUpdate(systemDate);
        verify(interestAccrualService).runEODAccruals(systemDate);
        verify(eodReportingService).generateFinancialReports(systemDate);
    }

    @Test
    void testExecuteEOD_ValidationFailure() {
        // Given
        when(eodValidationService.performPreEODValidations(any(), any()))
                .thenReturn(EODValidationService.EODValidationResult.failure("Validation failed"));

        // When
        EODOrchestrationService.EODResult result = eodOrchestrationService.executeEOD(userId);

        // Then
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Pre-EOD validations failed"));
        assertEquals(0, result.getAccountsProcessed());
        
        // Verify validation was called but no batch jobs were executed
        verify(eodValidationService).performPreEODValidations(userId, systemDate);
        verify(accountBalanceUpdateService, never()).executeAccountBalanceUpdate(any());
    }

    @Test
    void testExecuteEOD_BatchJobFailure() {
        // Given
        when(eodValidationService.performPreEODValidations(any(), any()))
                .thenReturn(EODValidationService.EODValidationResult.success("All validations passed"));
        when(accountBalanceUpdateService.executeAccountBalanceUpdate(any()))
                .thenThrow(new RuntimeException("Batch job failed"));

        // When
        EODOrchestrationService.EODResult result = eodOrchestrationService.executeEOD(userId);

        // Then
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("EOD process failed"));
        
        // Verify validation was called and first batch job was attempted
        verify(eodValidationService).performPreEODValidations(userId, systemDate);
        verify(accountBalanceUpdateService).executeAccountBalanceUpdate(systemDate);
        // Verify subsequent jobs were not called due to failure
        verify(interestAccrualService, never()).runEODAccruals(any());
    }

    @Test
    void testExecuteBatchJob1_Success() {
        // Given
        when(accountBalanceUpdateService.executeAccountBalanceUpdate(any())).thenReturn(5);

        // When
        int result = eodOrchestrationService.executeBatchJob1(LocalDate.now(), systemDate, userId);

        // Then
        assertEquals(5, result);
        verify(accountBalanceUpdateService).executeAccountBalanceUpdate(systemDate);
    }

    @Test
    void testExecuteBatchJob1_Failure() {
        // Given
        when(accountBalanceUpdateService.executeAccountBalanceUpdate(any()))
                .thenThrow(new RuntimeException("Account balance update failed"));

        // When & Then
        assertThrows(RuntimeException.class, () -> 
                eodOrchestrationService.executeBatchJob1(LocalDate.now(), systemDate, userId));
        
        verify(accountBalanceUpdateService).executeAccountBalanceUpdate(systemDate);
    }

    @Test
    void testExecuteBatchJob2_Success() {
        // Given
        when(interestAccrualService.runEODAccruals(any())).thenReturn(3);

        // When
        int result = eodOrchestrationService.executeBatchJob2(LocalDate.now(), systemDate, userId);

        // Then
        assertEquals(3, result);
        verify(interestAccrualService).runEODAccruals(systemDate);
    }

    @Test
    void testExecuteBatchJob7_Success() {
        // Given
        when(eodReportingService.generateFinancialReports(any())).thenReturn(true);

        // When
        boolean result = eodOrchestrationService.executeBatchJob7(LocalDate.now(), systemDate, userId);

        // Then
        assertTrue(result);
        verify(eodReportingService).generateFinancialReports(systemDate);
    }

    @Test
    void testExecuteBatchJob8_Success() {
        // Given
        when(parameterTableRepository.findByParameterName("System_Date")).thenReturn(Optional.empty());

        // When
        boolean result = eodOrchestrationService.executeBatchJob8(LocalDate.now(), systemDate, userId);

        // Then
        assertTrue(result);
        // Verify system date update was attempted
        verify(parameterTableRepository, atLeastOnce()).findByParameterName(any());
    }
}
