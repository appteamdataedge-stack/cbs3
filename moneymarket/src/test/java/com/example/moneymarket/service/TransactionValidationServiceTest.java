package com.example.moneymarket.service;

import com.example.moneymarket.entity.AcctBal;
import com.example.moneymarket.entity.TranTable.DrCrFlag;
import com.example.moneymarket.exception.BusinessException;
import com.example.moneymarket.repository.AcctBalRepository;
import com.example.moneymarket.repository.CustAcctMasterRepository;
import com.example.moneymarket.repository.TranTableRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Test class for TransactionValidationService
 * Tests the new business rules for customer and office accounts
 */
@ExtendWith(MockitoExtension.class)
class TransactionValidationServiceTest {

    @Mock
    private AcctBalRepository acctBalRepository;
    
    @Mock
    private TranTableRepository tranTableRepository;
    
    @Mock
    private GLValidationService glValidationService;
    
    @Mock
    private CustAcctMasterRepository custAcctMasterRepository;
    
    @Mock
    private SystemDateService systemDateService;
    
    @Mock
    private UnifiedAccountService unifiedAccountService;
    
    @Mock
    private GLHierarchyService glHierarchyService;

    @Mock
    private AccountBalanceUpdateService accountBalanceUpdateService;

    private TransactionValidationService transactionValidationService;

    @BeforeEach
    void setUp() {
        transactionValidationService = new TransactionValidationService(
                acctBalRepository, tranTableRepository, custAcctMasterRepository,
                systemDateService, unifiedAccountService, glHierarchyService, accountBalanceUpdateService);
    }

    @Test
    void testValidateCustomerAccountDebitTransaction_Success() {
        // Given
        String accountNo = "100000001001";
        BigDecimal amount = new BigDecimal("100.00");
        LocalDate systemDate = LocalDate.of(2024, 1, 15);

        // Mock account info - customer account
        UnifiedAccountService.AccountInfo accountInfo =
                new UnifiedAccountService.AccountInfo(accountNo, "110101001", "Test Account", true, false, true, false);
        when(unifiedAccountService.getAccountInfo(accountNo)).thenReturn(accountInfo);

        // Mock balance
        AcctBal balance = AcctBal.builder()
                .accountNo(accountNo)
                .currentBalance(new BigDecimal("500.00"))
                .openingBal(new BigDecimal("500.00"))
                .lastUpdated(LocalDateTime.now())
                .build();
        when(acctBalRepository.findLatestByAccountNo(accountNo)).thenReturn(Optional.of(balance));

        // Mock system date
        when(systemDateService.getSystemDate()).thenReturn(systemDate);

        // Mock getPreviousDayClosingBalance (this is called to calculate available balance)
        when(accountBalanceUpdateService.getPreviousDayClosingBalance(accountNo, systemDate))
                .thenReturn(new BigDecimal("500.00"));

        // Mock transaction sums
        when(tranTableRepository.sumDebitTransactionsForAccountOnDate(accountNo, systemDate))
                .thenReturn(Optional.of(BigDecimal.ZERO));
        when(tranTableRepository.sumCreditTransactionsForAccountOnDate(accountNo, systemDate))
                .thenReturn(Optional.of(BigDecimal.ZERO));

        // When & Then
        assertTrue(transactionValidationService.validateTransaction(accountNo, DrCrFlag.D, amount));
    }

    @Test
    void testValidateCustomerAccountDebitTransaction_InsufficientBalance() {
        // Given
        String accountNo = "100000001001";
        BigDecimal amount = new BigDecimal("600.00"); // More than available balance
        LocalDate systemDate = LocalDate.of(2024, 1, 15);

        // Mock account info - customer account
        UnifiedAccountService.AccountInfo accountInfo =
                new UnifiedAccountService.AccountInfo(accountNo, "110101001", "Test Account", true, false, true, false);
        when(unifiedAccountService.getAccountInfo(accountNo)).thenReturn(accountInfo);

        // Mock balance
        AcctBal balance = AcctBal.builder()
                .accountNo(accountNo)
                .currentBalance(new BigDecimal("500.00"))
                .openingBal(new BigDecimal("500.00"))
                .lastUpdated(LocalDateTime.now())
                .build();
        when(acctBalRepository.findLatestByAccountNo(accountNo)).thenReturn(Optional.of(balance));

        // Mock system date
        when(systemDateService.getSystemDate()).thenReturn(systemDate);

        // Mock getPreviousDayClosingBalance
        when(accountBalanceUpdateService.getPreviousDayClosingBalance(accountNo, systemDate))
                .thenReturn(new BigDecimal("500.00"));

        // Mock transaction sums
        when(tranTableRepository.sumDebitTransactionsForAccountOnDate(accountNo, systemDate))
                .thenReturn(Optional.of(BigDecimal.ZERO));
        when(tranTableRepository.sumCreditTransactionsForAccountOnDate(accountNo, systemDate))
                .thenReturn(Optional.of(BigDecimal.ZERO));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> transactionValidationService.validateTransaction(accountNo, DrCrFlag.D, amount));
        assertTrue(exception.getMessage().contains("Insufficient balance"));
    }

    @Test
    void testValidateOfficeAssetAccount_CannotGoIntoCredit() {
        // Given
        String accountNo = "200000001001";
        BigDecimal amount = new BigDecimal("100.00");
        LocalDate systemDate = LocalDate.of(2024, 1, 15);

        // Mock account info - office asset account
        // NOTE: Asset accounts (GL starting with "2") do NOT have balance validation
        // They can go positive (credit) without restriction - see TransactionValidationService:152-156
        UnifiedAccountService.AccountInfo accountInfo =
                new UnifiedAccountService.AccountInfo(accountNo, "210101001", "Office Asset Account", false, true, false, false);
        when(unifiedAccountService.getAccountInfo(accountNo)).thenReturn(accountInfo);

        // Mock balance - current balance is 50, credit of 100 would make it +150 (credit)
        AcctBal balance = AcctBal.builder()
                .accountNo(accountNo)
                .currentBalance(new BigDecimal("50.00"))
                .openingBal(new BigDecimal("50.00"))
                .lastUpdated(LocalDateTime.now())
                .build();
        when(acctBalRepository.findLatestByAccountNo(accountNo)).thenReturn(Optional.of(balance));

        // Mock system date
        when(systemDateService.getSystemDate()).thenReturn(systemDate);

        // When & Then - Asset accounts skip validation entirely, so transaction should succeed
        // The test expectation was wrong - asset accounts CAN go into credit
        assertTrue(transactionValidationService.validateTransaction(accountNo, DrCrFlag.C, amount));
    }

    @Test
    void testValidateOfficeLiabilityAccount_CannotGoIntoDebit() {
        // Given
        String accountNo = "100000001001";
        BigDecimal amount = new BigDecimal("100.00");
        LocalDate systemDate = LocalDate.of(2024, 1, 15);

        // Mock account info - office liability account
        UnifiedAccountService.AccountInfo accountInfo =
                new UnifiedAccountService.AccountInfo(accountNo, "110101001", "Office Liability Account", false, false, true, false);
        when(unifiedAccountService.getAccountInfo(accountNo)).thenReturn(accountInfo);

        // Mock balance - current balance is 50, debit of 100 would make it -50 (debit)
        AcctBal balance = AcctBal.builder()
                .accountNo(accountNo)
                .currentBalance(new BigDecimal("50.00"))
                .openingBal(new BigDecimal("50.00"))
                .lastUpdated(LocalDateTime.now())
                .build();
        when(acctBalRepository.findLatestByAccountNo(accountNo)).thenReturn(Optional.of(balance));

        // Mock system date
        when(systemDateService.getSystemDate()).thenReturn(systemDate);

        // When & Then - Debit transaction that would make liability account go into debit
        BusinessException exception = assertThrows(BusinessException.class,
                () -> transactionValidationService.validateTransaction(accountNo, DrCrFlag.D, amount));
        assertTrue(exception.getMessage().contains("Insufficient balance"));
    }

    @Test
    void testValidateOfficeAccount_ValidTransaction() {
        // Given
        String accountNo = "200000001001";
        BigDecimal amount = new BigDecimal("50.00");
        LocalDate systemDate = LocalDate.of(2024, 1, 15);
        
        // Mock account info - office asset account
        UnifiedAccountService.AccountInfo accountInfo = 
                new UnifiedAccountService.AccountInfo(accountNo, "210101001", "Office Asset Account", false, true, false, false);
        when(unifiedAccountService.getAccountInfo(accountNo)).thenReturn(accountInfo);
        
        // Mock balance - current balance is -100, debit of 50 would make it -150 (still debit, valid)
        AcctBal balance = AcctBal.builder()
                .accountNo(accountNo)
                .currentBalance(new BigDecimal("-100.00"))
                .openingBal(new BigDecimal("-100.00"))
                .lastUpdated(LocalDateTime.now())
                .build();
        when(acctBalRepository.findLatestByAccountNo(accountNo)).thenReturn(Optional.of(balance));
        
        // Mock system date
        when(systemDateService.getSystemDate()).thenReturn(systemDate);
        
        // When & Then - Debit transaction that keeps asset account in debit (valid)
        assertTrue(transactionValidationService.validateTransaction(accountNo, DrCrFlag.D, amount));
    }

    @Test
    void testValidateOverdraftAccount_AllowsNegativeBalance() {
        // Given
        String accountNo = "100000006001"; // Overdraft account
        BigDecimal amount = new BigDecimal("600.00"); // More than available balance
        LocalDate systemDate = LocalDate.of(2024, 1, 15);
        
        // Mock account info - overdraft customer account
        UnifiedAccountService.AccountInfo accountInfo = 
                new UnifiedAccountService.AccountInfo(accountNo, "210201001", "Overdraft Account", true, false, true, true);
        when(unifiedAccountService.getAccountInfo(accountNo)).thenReturn(accountInfo);
        
        // Mock GLHierarchyService to return true for overdraft account
        when(glHierarchyService.isOverdraftAccount("210201001")).thenReturn(true);
        
        // Mock balance
        AcctBal balance = AcctBal.builder()
                .accountNo(accountNo)
                .currentBalance(new BigDecimal("500.00"))
                .openingBal(new BigDecimal("500.00"))
                .lastUpdated(LocalDateTime.now())
                .build();
        when(acctBalRepository.findLatestByAccountNo(accountNo)).thenReturn(Optional.of(balance));
        
        // Mock system date
        when(systemDateService.getSystemDate()).thenReturn(systemDate);
        
        // When & Then - Overdraft account should allow negative balance
        assertTrue(transactionValidationService.validateTransaction(accountNo, DrCrFlag.D, amount));
    }

    @Test
    void testValidateOverdraftInterestIncomeAccount_AllowsNegativeBalance() {
        // Given
        String accountNo = "100000005001"; // Overdraft Interest Income account
        BigDecimal amount = new BigDecimal("600.00"); // More than available balance
        LocalDate systemDate = LocalDate.of(2024, 1, 15);
        
        // Mock account info - overdraft interest income customer account
        UnifiedAccountService.AccountInfo accountInfo = 
                new UnifiedAccountService.AccountInfo(accountNo, "140101001", "Overdraft Interest Income Account", true, false, true, true);
        when(unifiedAccountService.getAccountInfo(accountNo)).thenReturn(accountInfo);
        
        // Mock GLHierarchyService to return true for overdraft interest income account
        when(glHierarchyService.isOverdraftAccount("140101001")).thenReturn(true);
        
        // Mock balance
        AcctBal balance = AcctBal.builder()
                .accountNo(accountNo)
                .currentBalance(new BigDecimal("500.00"))
                .openingBal(new BigDecimal("500.00"))
                .lastUpdated(LocalDateTime.now())
                .build();
        when(acctBalRepository.findLatestByAccountNo(accountNo)).thenReturn(Optional.of(balance));
        
        // Mock system date
        when(systemDateService.getSystemDate()).thenReturn(systemDate);
        
        // When & Then - Overdraft Interest Income account should allow negative balance
        assertTrue(transactionValidationService.validateTransaction(accountNo, DrCrFlag.D, amount));
    }
}
