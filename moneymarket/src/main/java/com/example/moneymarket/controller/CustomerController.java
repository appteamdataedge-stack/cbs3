package com.example.moneymarket.controller;

import com.example.moneymarket.dto.CustomerRequestDTO;
import com.example.moneymarket.dto.CustomerResponseDTO;
import com.example.moneymarket.dto.CustomerVerificationDTO;
import com.example.moneymarket.service.CustomerService;
import com.example.moneymarket.validation.CustomerValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for customer operations
 */
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerValidator customerValidator;

    /**
     * Initialize validator for customer request
     * 
     * @param binder The WebDataBinder
     */
    @InitBinder("customerRequestDTO")
    public void initBinder(WebDataBinder binder) {
        binder.addValidators(customerValidator);
    }

    /**
     * Create a new customer
     * 
     * @param customerRequestDTO The customer data
     * @param bindingResult Validation result
     * @return The created customer
     */
    @PostMapping
    public ResponseEntity<CustomerResponseDTO> createCustomer(
            @Valid @RequestBody CustomerRequestDTO customerRequestDTO,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new com.example.moneymarket.exception.BusinessException(
                    bindingResult.getAllErrors().get(0).getDefaultMessage());
        }
        
        CustomerResponseDTO createdCustomer = customerService.createCustomer(customerRequestDTO);
        return new ResponseEntity<>(createdCustomer, HttpStatus.CREATED);
    }

    /**
     * Update an existing customer
     * 
     * @param id The customer ID
     * @param customerRequestDTO The customer data
     * @param bindingResult Validation result
     * @return The updated customer
     */
    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponseDTO> updateCustomer(
            @PathVariable Integer id,
            @Valid @RequestBody CustomerRequestDTO customerRequestDTO,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new com.example.moneymarket.exception.BusinessException(
                    bindingResult.getAllErrors().get(0).getDefaultMessage());
        }
        
        CustomerResponseDTO updatedCustomer = customerService.updateCustomer(id, customerRequestDTO);
        return ResponseEntity.ok(updatedCustomer);
    }

    /**
     * Get a customer by ID
     * 
     * @param id The customer ID
     * @return The customer
     */
    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponseDTO> getCustomer(@PathVariable Integer id) {
        CustomerResponseDTO customer = customerService.getCustomer(id);
        return ResponseEntity.ok(customer);
    }

    /**
     * Get all customers with pagination
     * 
     * @param pageable The pagination information
     * @param search The optional search term
     * @return Page of customers
     */
    @GetMapping
    public ResponseEntity<Page<CustomerResponseDTO>> getAllCustomers(
            @RequestParam(required = false) String search,
            Pageable pageable) {
        
        Page<CustomerResponseDTO> customers;
        if (search != null && !search.trim().isEmpty()) {
            // If search parameter is provided, use search functionality
            customers = customerService.searchCustomers(search, pageable);
        } else {
            // Otherwise, get all customers
            customers = customerService.getAllCustomers(pageable);
        }
        
        return ResponseEntity.ok(customers);
    }

    /**
     * Verify a customer
     * 
     * @param id The customer ID
     * @param verificationDTO The verification data
     * @return The verified customer
     */
    @PostMapping("/{id}/verify")
    public ResponseEntity<CustomerResponseDTO> verifyCustomer(
            @PathVariable Integer id,
            @Valid @RequestBody CustomerVerificationDTO verificationDTO) {
        CustomerResponseDTO verifiedCustomer = customerService.verifyCustomer(id, verificationDTO);
        return ResponseEntity.ok(verifiedCustomer);
    }
}
