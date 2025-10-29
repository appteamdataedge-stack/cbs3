package com.example.moneymarket.service;

import com.example.moneymarket.entity.AccountSeq;
import com.example.moneymarket.entity.CustMaster;
import com.example.moneymarket.entity.ProdMaster;
import com.example.moneymarket.entity.SubProdMaster;
import com.example.moneymarket.exception.BusinessException;
import com.example.moneymarket.repository.AccountSeqRepository;
import com.example.moneymarket.repository.CustAcctMasterRepository;
import com.example.moneymarket.repository.GLSetupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AccountNumberServiceTest {

    @Mock
    private AccountSeqRepository accountSeqRepository;

    @Mock
    private GLSetupRepository glSetupRepository;
    
    @Mock
    private CustAcctMasterRepository custAcctMasterRepository;

    @InjectMocks
    private AccountNumberService accountNumberService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void generateAccountNumber_Success() {
        // Arrange
        String glNum = "110101001";
        AccountSeq accountSeq = new AccountSeq();
        accountSeq.setGlNum(glNum);
        accountSeq.setSeqNumber(1);
        accountSeq.setLastUpdated(LocalDateTime.now());

        when(glSetupRepository.existsById(glNum)).thenReturn(true);
        when(accountSeqRepository.findByGlNumWithLock(glNum)).thenReturn(Optional.of(accountSeq));
        when(accountSeqRepository.save(any(AccountSeq.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        String accountNumber = accountNumberService.generateAccountNumber(glNum);

        // Assert
        assertNotNull(accountNumber);
        assertEquals(glNum + "002", accountNumber);
        verify(accountSeqRepository).findByGlNumWithLock(glNum);
        verify(accountSeqRepository).save(accountSeq);
        assertEquals(2, accountSeq.getSeqNumber());
    }

    @Test
    void generateAccountNumber_InitializeSequence() {
        // Arrange
        String glNum = "110101001";

        when(glSetupRepository.existsById(glNum)).thenReturn(true);
        when(accountSeqRepository.findByGlNumWithLock(glNum)).thenReturn(Optional.empty());
        when(accountSeqRepository.save(any(AccountSeq.class))).thenAnswer(invocation -> {
            AccountSeq savedSeq = invocation.getArgument(0);
            savedSeq.setSeqNumber(1);
            return savedSeq;
        });

        // Act
        String accountNumber = accountNumberService.generateAccountNumber(glNum);

        // Assert
        assertNotNull(accountNumber);
        assertEquals(glNum + "001", accountNumber);
        verify(accountSeqRepository).findByGlNumWithLock(glNum);
        verify(accountSeqRepository).save(any(AccountSeq.class));
    }

    @Test
    void generateAccountNumber_InvalidGLNumber() {
        // Arrange
        String glNum = "999999999";

        when(glSetupRepository.existsById(glNum)).thenReturn(false);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            accountNumberService.generateAccountNumber(glNum);
        });

        assertEquals("Cannot generate account number: GL Number " + glNum + " does not exist", exception.getMessage());
        verify(accountSeqRepository, never()).findByGlNumWithLock(anyString());
        verify(accountSeqRepository, never()).save(any(AccountSeq.class));
    }

    @Test
    void generateAccountNumber_SequenceOverflow() {
        // Arrange
        String glNum = "110101001";
        AccountSeq accountSeq = new AccountSeq();
        accountSeq.setGlNum(glNum);
        accountSeq.setSeqNumber(999);
        accountSeq.setLastUpdated(LocalDateTime.now());

        when(glSetupRepository.existsById(glNum)).thenReturn(true);
        when(accountSeqRepository.findByGlNumWithLock(glNum)).thenReturn(Optional.of(accountSeq));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            accountNumberService.generateAccountNumber(glNum);
        });

        assertEquals("Account number sequence for GL " + glNum + " has reached its maximum (999)", exception.getMessage());
        verify(accountSeqRepository).findByGlNumWithLock(glNum);
        verify(accountSeqRepository, never()).save(any(AccountSeq.class));
    }

    @Test
    void generateCustomerAccountNumber_Success_GL110101000() {
        // Arrange
        CustMaster customer = CustMaster.builder()
                .custId(10002345)
                .build();
        
        ProdMaster product = ProdMaster.builder()
                .productId(1)
                .cumGLNum("110101000")
                .build();
        
        SubProdMaster subProduct = SubProdMaster.builder()
                .subProductId(1)
                .product(product)
                .cumGLNum("110101001")
                .build();

        when(custAcctMasterRepository.findMaxSequenceForCustomerAndProductType(10002345, "1"))
                .thenReturn(null);

        // Act
        String accountNumber = accountNumberService.generateCustomerAccountNumber(customer, subProduct);

        // Assert
        assertNotNull(accountNumber);
        assertEquals("100023451001", accountNumber); // 8 digits custId + 1 (9th digit) + 001 (sequence)
        verify(custAcctMasterRepository).findMaxSequenceForCustomerAndProductType(10002345, "1");
    }

    @Test
    void generateCustomerAccountNumber_Success_GL110102000() {
        // Arrange
        CustMaster customer = CustMaster.builder()
                .custId(10002345)
                .build();
        
        ProdMaster product = ProdMaster.builder()
                .productId(1)
                .cumGLNum("110102000")
                .build();
        
        SubProdMaster subProduct = SubProdMaster.builder()
                .subProductId(1)
                .product(product)
                .cumGLNum("110102001")
                .build();

        when(custAcctMasterRepository.findMaxSequenceForCustomerAndProductType(10002345, "2"))
                .thenReturn(1);

        // Act
        String accountNumber = accountNumberService.generateCustomerAccountNumber(customer, subProduct);

        // Assert
        assertNotNull(accountNumber);
        assertEquals("100023452002", accountNumber); // 8 digits custId + 2 (9th digit) + 002 (sequence)
        verify(custAcctMasterRepository).findMaxSequenceForCustomerAndProductType(10002345, "2");
    }

    @Test
    void generateCustomerAccountNumber_Success_GL210201000() {
        // Arrange
        CustMaster customer = CustMaster.builder()
                .custId(10002345)
                .build();
        
        ProdMaster product = ProdMaster.builder()
                .productId(1)
                .cumGLNum("210201000")
                .build();
        
        SubProdMaster subProduct = SubProdMaster.builder()
                .subProductId(1)
                .product(product)
                .cumGLNum("210201001")
                .build();

        when(custAcctMasterRepository.findMaxSequenceForCustomerAndProductType(10002345, "6"))
                .thenReturn(5);

        // Act
        String accountNumber = accountNumberService.generateCustomerAccountNumber(customer, subProduct);

        // Assert
        assertNotNull(accountNumber);
        assertEquals("100023456006", accountNumber); // 8 digits custId + 6 (9th digit) + 006 (sequence)
        verify(custAcctMasterRepository).findMaxSequenceForCustomerAndProductType(10002345, "6");
    }

    @Test
    void generateCustomerAccountNumber_InvalidProductGLNum() {
        // Arrange
        CustMaster customer = CustMaster.builder()
                .custId(10002345)
                .build();
        
        ProdMaster product = ProdMaster.builder()
                .productId(1)
                .cumGLNum("999999999") // Invalid GL_Num
                .build();
        
        SubProdMaster subProduct = SubProdMaster.builder()
                .subProductId(1)
                .product(product)
                .cumGLNum("999999001")
                .build();

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            accountNumberService.generateCustomerAccountNumber(customer, subProduct);
        });

        assertTrue(exception.getMessage().contains("Cannot determine product type code for Product GL_Num: 999999999"));
        verify(custAcctMasterRepository, never()).findMaxSequenceForCustomerAndProductType(any(), any());
    }

    @Test
    void generateCustomerAccountNumber_MissingProduct() {
        // Arrange
        CustMaster customer = CustMaster.builder()
                .custId(10002345)
                .build();
        
        SubProdMaster subProduct = SubProdMaster.builder()
                .subProductId(1)
                .product(null) // Missing product
                .cumGLNum("110101001")
                .build();

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            accountNumberService.generateCustomerAccountNumber(customer, subProduct);
        });

        assertEquals("Cannot generate account number: Product information is missing", exception.getMessage());
        verify(custAcctMasterRepository, never()).findMaxSequenceForCustomerAndProductType(any(), any());
    }
}
