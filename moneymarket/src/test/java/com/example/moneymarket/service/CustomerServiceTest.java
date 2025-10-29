package com.example.moneymarket.service;

import com.example.moneymarket.dto.CustomerRequestDTO;
import com.example.moneymarket.dto.CustomerResponseDTO;
import com.example.moneymarket.dto.CustomerVerificationDTO;
import com.example.moneymarket.entity.CustMaster;
import com.example.moneymarket.exception.BusinessException;
import com.example.moneymarket.exception.ResourceNotFoundException;
import com.example.moneymarket.repository.CustMasterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CustomerServiceTest {

    @Mock
    private CustMasterRepository custMasterRepository;
    
    @Mock
    private CustomerIdService customerIdService;

    @InjectMocks
    private CustomerService customerService;

    private CustomerRequestDTO customerRequestDTO;
    private CustMaster customerEntity;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup test data
        customerRequestDTO = CustomerRequestDTO.builder()
                .extCustId("EXT123")
                .custType(CustMaster.CustomerType.Individual)
                .firstName("John")
                .lastName("Doe")
                .address1("123 Main St")
                .mobile("1234567890")
                .makerId("ADMIN")
                .build();

        customerEntity = CustMaster.builder()
                .custId(1)
                .extCustId("EXT123")
                .custType(CustMaster.CustomerType.Individual)
                .firstName("John")
                .lastName("Doe")
                .address1("123 Main St")
                .mobile("1234567890")
                .makerId("ADMIN")
                .entryDate(LocalDate.now())
                .entryTime(LocalTime.now())
                .build();
    }

    @Test
    void createCustomer_Success() {
        // Arrange
        when(custMasterRepository.existsByExtCustId(customerRequestDTO.getExtCustId())).thenReturn(false);
        when(customerIdService.generateCustomerId(customerRequestDTO.getCustType())).thenReturn(1);
        when(custMasterRepository.save(any(CustMaster.class))).thenReturn(customerEntity);

        // Act
        CustomerResponseDTO result = customerService.createCustomer(customerRequestDTO);

        // Assert
        assertNotNull(result);
        assertEquals(customerEntity.getCustId(), result.getCustId());
        assertEquals(customerEntity.getExtCustId(), result.getExtCustId());
        verify(custMasterRepository).existsByExtCustId(customerRequestDTO.getExtCustId());
        verify(custMasterRepository).save(any(CustMaster.class));
    }

    @Test
    void createCustomer_DuplicateExtCustId() {
        // Arrange
        when(custMasterRepository.existsByExtCustId(customerRequestDTO.getExtCustId())).thenReturn(true);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            customerService.createCustomer(customerRequestDTO);
        });

        assertEquals("External Customer ID already exists", exception.getMessage());
        verify(custMasterRepository).existsByExtCustId(customerRequestDTO.getExtCustId());
        verify(custMasterRepository, never()).save(any(CustMaster.class));
    }

    @Test
    void createCustomer_IndividualWithoutFirstName() {
        // Arrange
        customerRequestDTO.setFirstName(null);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            customerService.createCustomer(customerRequestDTO);
        });

        assertEquals("First Name is required for Individual customer", exception.getMessage());
        verify(custMasterRepository, never()).save(any(CustMaster.class));
    }

    @Test
    void getCustomer_Success() {
        // Arrange
        when(custMasterRepository.findById(1)).thenReturn(Optional.of(customerEntity));

        // Act
        CustomerResponseDTO result = customerService.getCustomer(1);

        // Assert
        assertNotNull(result);
        assertEquals(customerEntity.getCustId(), result.getCustId());
        verify(custMasterRepository).findById(1);
    }

    @Test
    void getCustomer_NotFound() {
        // Arrange
        when(custMasterRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            customerService.getCustomer(999);
        });

        verify(custMasterRepository).findById(999);
    }

    @Test
    void getAllCustomers() {
        // Arrange
        List<CustMaster> customers = Arrays.asList(customerEntity);
        Page<CustMaster> pagedCustomers = new PageImpl<>(customers);
        Pageable pageable = Pageable.unpaged();

        when(custMasterRepository.findAll(pageable)).thenReturn(pagedCustomers);

        // Act
        Page<CustomerResponseDTO> result = customerService.getAllCustomers(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(custMasterRepository).findAll(pageable);
    }

    @Test
    void verifyCustomer_Success() {
        // Arrange
        CustomerVerificationDTO verificationDTO = new CustomerVerificationDTO("VERIFIER");
        when(custMasterRepository.findById(1)).thenReturn(Optional.of(customerEntity));
        when(custMasterRepository.save(any(CustMaster.class))).thenReturn(customerEntity);

        // Act
        CustomerResponseDTO result = customerService.verifyCustomer(1, verificationDTO);

        // Assert
        assertNotNull(result);
        assertEquals("VERIFIER", result.getVerifierId());
        assertTrue(result.isVerified());
        verify(custMasterRepository).findById(1);
        verify(custMasterRepository).save(any(CustMaster.class));
    }

    @Test
    void verifyCustomer_MakerVerifierSame() {
        // Arrange
        CustomerVerificationDTO verificationDTO = new CustomerVerificationDTO("ADMIN");
        when(custMasterRepository.findById(1)).thenReturn(Optional.of(customerEntity));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            customerService.verifyCustomer(1, verificationDTO);
        });

        assertEquals("Maker cannot verify their own record", exception.getMessage());
        verify(custMasterRepository).findById(1);
        verify(custMasterRepository, never()).save(any(CustMaster.class));
    }
}
