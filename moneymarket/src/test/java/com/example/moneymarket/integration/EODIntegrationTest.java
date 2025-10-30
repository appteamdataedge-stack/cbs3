package com.example.moneymarket.integration;

import com.example.moneymarket.entity.*;
import com.example.moneymarket.repository.*;
import com.example.moneymarket.service.EODOrchestrationService;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EODIntegrationTest {

    @Autowired
    private EODOrchestrationService eodOrchestrationService;

    @Autowired
    private ParameterTableRepository parameterTableRepository;

    @Autowired
    private EODLogTableRepository eodLogTableRepository;

    @Autowired
    private TranTableRepository tranTableRepository;

    @Autowired
    private CustAcctMasterRepository custAcctMasterRepository;

    @Autowired
    private AcctBalRepository acctBalRepository;

    @Autowired
    private GLBalanceRepository glBalanceRepository;

    @Autowired
    private GLMovementRepository glMovementRepository;

    @Autowired
    private SubProdMasterRepository subProdMasterRepository;

    @Autowired
    private CustMasterRepository custMasterRepository;

    private LocalDate systemDate;
    private String userId;

    @BeforeEach
    void setUp() {
        systemDate = LocalDate.of(2024, 1, 15);
        userId = "ADMIN";
        
        // Setup test data
        setupTestData();
    }

    @Test
    void testFullEODProcess() {
        // Given - Test data is set up in setUp()
        
        // When
        EODOrchestrationService.EODResult result = eodOrchestrationService.executeEOD(userId);
        
        // Then
        assertTrue(result.isSuccess());
        assertTrue(result.getAccountsProcessed() >= 0);

        // Verify system date was incremented
        LocalDate newSystemDate = eodOrchestrationService.getSystemDate();
        assertEquals(systemDate.plusDays(1), newSystemDate);

        // Verify EOD log entries were created (use systemDate, not LocalDate.now())
        List<EODLogTable> logEntries = eodLogTableRepository.findByEodDate(systemDate);
        assertFalse(logEntries.isEmpty());
    }

    @Test
    void testEODProcessWithValidationFailure() {
        // Given - Create transactions in Entry status to cause validation failure
        TranTable entryTransaction = TranTable.builder()
                .tranId("TXN001")
                .tranDate(systemDate)
                .valueDate(systemDate)
                .tranStatus(TranTable.TranStatus.Entry)
                .drCrFlag(TranTable.DrCrFlag.D)
                .accountNo("ACC001")
                .tranCcy("BDT")
                .fcyAmt(new BigDecimal("1000.00"))
                .exchangeRate(new BigDecimal("1.0000"))
                .lcyAmt(new BigDecimal("1000.00"))
                .build();
        tranTableRepository.save(entryTransaction);
        
        // When
        EODOrchestrationService.EODResult result = eodOrchestrationService.executeEOD(userId);
        
        // Then
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Pre-EOD validations failed"));
        
        // Verify system date was NOT incremented
        LocalDate currentSystemDate = eodOrchestrationService.getSystemDate();
        assertEquals(systemDate, currentSystemDate);
    }

    @Test
    void testEODProcessWithDebitCreditMismatch() {
        // Given - Create unbalanced transactions
        TranTable debitTransaction = TranTable.builder()
                .tranId("TXN001")
                .tranDate(systemDate)
                .valueDate(systemDate)
                .tranStatus(TranTable.TranStatus.Verified)
                .drCrFlag(TranTable.DrCrFlag.D)
                .accountNo("ACC001")
                .tranCcy("BDT")
                .fcyAmt(new BigDecimal("1000.00"))
                .exchangeRate(new BigDecimal("1.0000"))
                .lcyAmt(new BigDecimal("1000.00"))
                .build();
        
        TranTable creditTransaction = TranTable.builder()
                .tranId("TXN002")
                .tranDate(systemDate)
                .valueDate(systemDate)
                .tranStatus(TranTable.TranStatus.Verified)
                .drCrFlag(TranTable.DrCrFlag.C)
                .accountNo("ACC002")
                .tranCcy("BDT")
                .fcyAmt(new BigDecimal("500.00")) // Different amount to cause imbalance
                .exchangeRate(new BigDecimal("1.0000"))
                .lcyAmt(new BigDecimal("500.00"))
                .build();
        
        tranTableRepository.save(debitTransaction);
        tranTableRepository.save(creditTransaction);
        
        // When
        EODOrchestrationService.EODResult result = eodOrchestrationService.executeEOD(userId);
        
        // Then
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Debit-Credit balance mismatch"));
    }

    private void setupTestData() {
        // Create system parameters
        ParameterTable systemDateParam = ParameterTable.builder()
                .parameterName("System_Date")
                .parameterValue(systemDate.toString())
                .parameterDescription("Current system date")
                .updatedBy("SYSTEM")
                .lastUpdated(java.time.LocalDateTime.now())
                .build();
        parameterTableRepository.save(systemDateParam);
        
        ParameterTable adminUserParam = ParameterTable.builder()
                .parameterName("EOD_Admin_User")
                .parameterValue("ADMIN")
                .parameterDescription("EOD admin user")
                .updatedBy("SYSTEM")
                .lastUpdated(java.time.LocalDateTime.now())
                .build();
        parameterTableRepository.save(adminUserParam);
        
        // Create balanced transactions for testing
        TranTable debitTransaction = TranTable.builder()
                .tranId("TXN001")
                .tranDate(systemDate)
                .valueDate(systemDate)
                .tranStatus(TranTable.TranStatus.Verified)
                .drCrFlag(TranTable.DrCrFlag.D)
                .accountNo("ACC001")
                .tranCcy("BDT")
                .fcyAmt(new BigDecimal("1000.00"))
                .exchangeRate(new BigDecimal("1.0000"))
                .lcyAmt(new BigDecimal("1000.00"))
                .build();
        
        TranTable creditTransaction = TranTable.builder()
                .tranId("TXN002")
                .tranDate(systemDate)
                .valueDate(systemDate)
                .tranStatus(TranTable.TranStatus.Verified)
                .drCrFlag(TranTable.DrCrFlag.C)
                .accountNo("ACC002")
                .tranCcy("BDT")
                .fcyAmt(new BigDecimal("1000.00"))
                .exchangeRate(new BigDecimal("1.0000"))
                .lcyAmt(new BigDecimal("1000.00"))
                .build();
        
        tranTableRepository.save(debitTransaction);
        tranTableRepository.save(creditTransaction);
    }
}
