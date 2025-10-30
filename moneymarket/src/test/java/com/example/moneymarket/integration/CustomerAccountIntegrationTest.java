package com.example.moneymarket.integration;

import com.example.moneymarket.dto.CustomerAccountRequestDTO;
import com.example.moneymarket.dto.CustomerAccountResponseDTO;
import com.example.moneymarket.entity.AcctBal;
import com.example.moneymarket.entity.CustAcctMaster;
import com.example.moneymarket.entity.CustAcctMaster.AccountStatus;
import com.example.moneymarket.entity.CustMaster;
import com.example.moneymarket.entity.SubProdMaster;
import com.example.moneymarket.entity.SubProdMaster.SubProductStatus;
import com.example.moneymarket.repository.CustAcctMasterRepository;
import com.example.moneymarket.repository.CustMasterRepository;
import com.example.moneymarket.repository.AcctBalRepository;
import com.example.moneymarket.repository.SubProdMasterRepository;
import com.example.moneymarket.service.AccountNumberService;
import com.example.moneymarket.service.BalanceService;
import com.example.moneymarket.service.CustomerAccountService;
import com.example.moneymarket.service.SystemDateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Integration test for customer account functionality
 * This test uses mocked repositories for testing the service integration
 */
@ActiveProfiles("test")
@Transactional
class CustomerAccountIntegrationTest {

    @Mock
    private CustAcctMasterRepository custAcctMasterRepository;
    
    @Mock
    private CustMasterRepository custMasterRepository;
    
    @Mock
    private SubProdMasterRepository subProdMasterRepository;
    
    @Mock
    private AccountNumberService accountNumberService;
    
    @Mock
    private AcctBalRepository acctBalRepository;

    @Mock
    private SystemDateService systemDateService;

    @Mock
    private BalanceService balanceService;

    @InjectMocks
    private CustomerAccountService customerAccountService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Mock SystemDateService
        when(systemDateService.getSystemDate()).thenReturn(LocalDate.now());
        when(systemDateService.getSystemDateTime()).thenReturn(java.time.LocalDateTime.now());
    }

    @Test
    void testCreateAndRetrieveAccount() {
        // Create a test account request
        CustomerAccountRequestDTO requestDTO = CustomerAccountRequestDTO.builder()
                .subProductId(1)
                .custId(1)
                .acctName("Test Account")
                .dateOpening(LocalDate.now())
                .branchCode("BR001")
                .accountStatus(AccountStatus.Active)
                .build();

        // Create mock entities
        CustMaster mockCustomer = CustMaster.builder()
                .custId(1)
                .firstName("Test")
                .lastName("Customer")
                .build();
                
        SubProdMaster mockSubProduct = SubProdMaster.builder()
                .subProductId(1)
                .subProductName("Test SubProduct")
                .cumGLNum("110101001")
                .subProductStatus(SubProdMaster.SubProductStatus.Active)
                .build();
                
        // Mock response for account number generation
        String mockAccountNo = "110101001001";
        
        // Configure the mock repository behavior
        when(custMasterRepository.findById(1)).thenReturn(java.util.Optional.of(mockCustomer));
        when(subProdMasterRepository.findByIdWithProduct(1)).thenReturn(java.util.Optional.of(mockSubProduct));
        when(accountNumberService.generateCustomerAccountNumber(any(), any())).thenReturn(mockAccountNo);
        
        // Mock account saving
        CustAcctMaster mockAccount = new CustAcctMaster();
        mockAccount.setAccountNo(mockAccountNo);
        mockAccount.setAcctName("Test Account");
        mockAccount.setCustomer(mockCustomer);
        mockAccount.setSubProduct(mockSubProduct);
        mockAccount.setAccountStatus(AccountStatus.Active);
        mockAccount.setDateOpening(LocalDate.now());
        mockAccount.setBranchCode("BR001");
        
        when(custAcctMasterRepository.save(any())).thenReturn(mockAccount);

        // Mock account balance
        AcctBal mockBalance = new AcctBal();
        mockBalance.setAccountNo("100000001001");
        mockBalance.setCurrentBalance(BigDecimal.ZERO);
        mockBalance.setAvailableBalance(BigDecimal.ZERO);
        mockBalance.setLastUpdated(LocalDateTime.now());
        
        when(acctBalRepository.save(any())).thenReturn(mockBalance);
        
        // Mock account retrieval
        when(custAcctMasterRepository.findById(mockAccountNo)).thenReturn(java.util.Optional.of(mockAccount));
        when(acctBalRepository.findLatestByAccountNo(mockAccountNo)).thenReturn(java.util.Optional.of(mockBalance));

        // Mock BalanceService for getComputedAccountBalance
        com.example.moneymarket.dto.AccountBalanceDTO mockBalanceDTO = com.example.moneymarket.dto.AccountBalanceDTO.builder()
                .accountNo(mockAccountNo)
                .currentBalance(BigDecimal.ZERO)
                .availableBalance(BigDecimal.ZERO)
                .computedBalance(BigDecimal.ZERO)
                .interestAccrued(BigDecimal.ZERO)
                .build();
        when(balanceService.getComputedAccountBalance(mockAccountNo)).thenReturn(mockBalanceDTO);
        
        // Create the account
        CustomerAccountResponseDTO createdAccount = customerAccountService.createAccount(requestDTO);
        
        // Verify the creation
        assertNotNull(createdAccount);
        assertEquals(mockAccountNo, createdAccount.getAccountNo());
        assertEquals("Test Account", createdAccount.getAcctName());
        
        // Retrieve and verify the account
        CustomerAccountResponseDTO retrievedAccount = customerAccountService.getAccount(mockAccountNo);
        assertNotNull(retrievedAccount);
        assertEquals(createdAccount.getAccountNo(), retrievedAccount.getAccountNo());
        assertEquals(createdAccount.getAcctName(), retrievedAccount.getAcctName());
        assertEquals(AccountStatus.Active, retrievedAccount.getAccountStatus());
    }
}
