package com.example.moneymarket.service;

import com.example.moneymarket.dto.CustomerRequestDTO;
import com.example.moneymarket.dto.CustomerResponseDTO;
import com.example.moneymarket.dto.CustomerVerificationDTO;
import com.example.moneymarket.entity.CustMaster;
import com.example.moneymarket.exception.BusinessException;
import com.example.moneymarket.exception.ResourceNotFoundException;
import com.example.moneymarket.repository.CustMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Service for customer operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustMasterRepository custMasterRepository;
    private final CustomerIdService customerIdService;

    /**
     * Create a new customer
     * 
     * @param customerRequestDTO The customer data
     * @return The created customer response
     */
    @Transactional
    public CustomerResponseDTO createCustomer(CustomerRequestDTO customerRequestDTO) {
        // If External Customer ID provided, ensure uniqueness
        if (customerRequestDTO.getExtCustId() != null && !customerRequestDTO.getExtCustId().trim().isEmpty()) {
            if (custMasterRepository.existsByExtCustId(customerRequestDTO.getExtCustId())) {
                throw new BusinessException("External Customer ID already exists");
            }
        }

        // Validate customer type specific fields
        validateCustomerTypeFields(customerRequestDTO);

        // Generate customer ID based on customer type
        Integer custId = customerIdService.generateCustomerId(customerRequestDTO.getCustType());

        // Map DTO to entity
        CustMaster customer = mapToEntity(customerRequestDTO);
        
        // Set the generated customer ID
        customer.setCustId(custId);

        // Set audit fields
        customer.setEntryDate(LocalDate.now());
        customer.setEntryTime(LocalTime.now());

        // Save the customer
        CustMaster savedCustomer = custMasterRepository.save(customer);
        log.info("Customer created with ID: {}", savedCustomer.getCustId());

        // Return the response with success message
        CustomerResponseDTO response = mapToResponse(savedCustomer);
        response.setMessage("Customer Id " + savedCustomer.getCustId() + " created");
        
        return response;
    }

    /**
     * Update an existing customer
     * 
     * @param custId The customer ID
     * @param customerRequestDTO The customer data
     * @return The updated customer response
     */
    @Transactional
    public CustomerResponseDTO updateCustomer(Integer custId, CustomerRequestDTO customerRequestDTO) {
        // Find the customer
        CustMaster customer = custMasterRepository.findById(custId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "ID", custId));

        // External customer ID uniqueness validation removed for updates

        // Validate customer type specific fields
        validateCustomerTypeFields(customerRequestDTO);

        // Update fields
        customer.setExtCustId(customerRequestDTO.getExtCustId());
        customer.setCustType(customerRequestDTO.getCustType());
        customer.setFirstName(customerRequestDTO.getFirstName());
        customer.setLastName(customerRequestDTO.getLastName());
        customer.setTradeName(customerRequestDTO.getTradeName());
        customer.setAddress1(customerRequestDTO.getAddress1());
        customer.setMobile(customerRequestDTO.getMobile());
        customer.setMakerId(customerRequestDTO.getMakerId());
        
        // After update, reset verification fields
        customer.setVerifierId(null);
        customer.setVerificationDate(null);
        customer.setVerificationTime(null);

        // Save the updated customer
        CustMaster updatedCustomer = custMasterRepository.save(customer);
        log.info("Customer updated with ID: {}", updatedCustomer.getCustId());

        // Return the response
        return mapToResponse(updatedCustomer);
    }

    /**
     * Get a customer by ID
     * 
     * @param custId The customer ID
     * @return The customer response
     */
    public CustomerResponseDTO getCustomer(Integer custId) {
        // Find the customer
        CustMaster customer = custMasterRepository.findById(custId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "ID", custId));

        // Return the response
        return mapToResponse(customer);
    }

    /**
     * Get all customers with pagination
     * 
     * @param pageable The pagination information
     * @return Page of customer responses
     */
    public Page<CustomerResponseDTO> getAllCustomers(Pageable pageable) {
        // Get the customers page
        Page<CustMaster> customers = custMasterRepository.findAll(pageable);

        // Map to response DTOs
        return customers.map(this::mapToResponse);
    }
    
    /**
     * Search customers by search term
     * 
     * @param searchTerm The search term
     * @param pageable The pagination information
     * @return Page of customer responses matching the search term
     */
    public Page<CustomerResponseDTO> searchCustomers(String searchTerm, Pageable pageable) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            // If search term is empty, return all customers
            return getAllCustomers(pageable);
        }
        
        // Search customers by the search term
        Page<CustMaster> customers = custMasterRepository.searchCustomers(searchTerm, pageable);
        
        // Map to response DTOs
        return customers.map(this::mapToResponse);
    }

    /**
     * Verify a customer
     * 
     * @param custId The customer ID
     * @param verificationDTO The verification data
     * @return The verified customer response
     */
    @Transactional
    public CustomerResponseDTO verifyCustomer(Integer custId, CustomerVerificationDTO verificationDTO) {
        // Find the customer
        CustMaster customer = custMasterRepository.findById(custId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "ID", custId));

        // Check if maker and verifier are different
        if (customer.getMakerId().equals(verificationDTO.getVerifierId())) {
            throw new BusinessException("Maker cannot verify their own record");
        }

        // Set verification fields
        customer.setVerifierId(verificationDTO.getVerifierId());
        customer.setVerificationDate(LocalDate.now());
        customer.setVerificationTime(LocalTime.now());

        // Save the verified customer
        CustMaster verifiedCustomer = custMasterRepository.save(customer);
        log.info("Customer verified with ID: {}", verifiedCustomer.getCustId());

        // Return the response
        return mapToResponse(verifiedCustomer);
    }

    /**
     * Validate customer type specific fields
     * 
     * @param customerRequestDTO The customer data
     */
    private void validateCustomerTypeFields(CustomerRequestDTO customerRequestDTO) {
        switch (customerRequestDTO.getCustType()) {
            case Individual:
                if (customerRequestDTO.getFirstName() == null || customerRequestDTO.getFirstName().trim().isEmpty()) {
                    throw new BusinessException("First Name is required for Individual customer");
                }
                if (customerRequestDTO.getLastName() == null || customerRequestDTO.getLastName().trim().isEmpty()) {
                    throw new BusinessException("Last Name is required for Individual customer");
                }
                break;
            case Corporate:
            case Bank:
                if (customerRequestDTO.getTradeName() == null || customerRequestDTO.getTradeName().trim().isEmpty()) {
                    throw new BusinessException("Trade Name is required for Corporate or Bank customer");
                }
                break;
        }
    }

    /**
     * Map DTO to entity
     * 
     * @param dto The DTO
     * @return The entity
     */
    private CustMaster mapToEntity(CustomerRequestDTO dto) {
        return CustMaster.builder()
                .extCustId(dto.getExtCustId())
                .custType(dto.getCustType())
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .tradeName(dto.getTradeName())
                .address1(dto.getAddress1())
                .mobile(dto.getMobile())
                .branchCode(dto.getBranchCode() != null ? dto.getBranchCode() : "001") // Default branch code
                .makerId(dto.getMakerId())
                .entryDate(LocalDate.now())
                .entryTime(LocalTime.now())
                .build();
    }

    /**
     * Map entity to response DTO
     * 
     * @param entity The entity
     * @return The response DTO
     */
    private CustomerResponseDTO mapToResponse(CustMaster entity) {
        return CustomerResponseDTO.builder()
                .custId(entity.getCustId())
                .extCustId(entity.getExtCustId())
                .custType(entity.getCustType())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .tradeName(entity.getTradeName())
                .address1(entity.getAddress1())
                .mobile(entity.getMobile())
                .branchCode(entity.getBranchCode() != null ? entity.getBranchCode() : "001") // Default branch code
                .makerId(entity.getMakerId())
                .entryDate(entity.getEntryDate())
                .entryTime(entity.getEntryTime())
                .verifierId(entity.getVerifierId())
                .verificationDate(entity.getVerificationDate())
                .verificationTime(entity.getVerificationTime())
                .verified(entity.getVerifierId() != null)
                .build();
    }
}
