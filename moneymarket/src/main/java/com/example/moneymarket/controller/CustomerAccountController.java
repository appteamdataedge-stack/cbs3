package com.example.moneymarket.controller;

import com.example.moneymarket.dto.CustomerAccountRequestDTO;
import com.example.moneymarket.dto.CustomerAccountResponseDTO;
import com.example.moneymarket.service.CustomerAccountService;
import com.example.moneymarket.validation.CustomerAccountValidator;
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
 * REST controller for customer account operations
 */
@RestController
@RequestMapping("/api/accounts/customer")
@RequiredArgsConstructor
public class CustomerAccountController {

    private final CustomerAccountService customerAccountService;
    private final CustomerAccountValidator customerAccountValidator;

    /**
     * Initialize validator for customer account request
     * 
     * @param binder The WebDataBinder
     */
    @InitBinder("customerAccountRequestDTO")
    public void initBinder(WebDataBinder binder) {
        binder.addValidators(customerAccountValidator);
    }

    /**
     * Create a new customer account
     * 
     * @param accountRequestDTO The account data
     * @param bindingResult Validation result
     * @return The created account
     */
    @PostMapping
    public ResponseEntity<CustomerAccountResponseDTO> createAccount(
            @Valid @RequestBody CustomerAccountRequestDTO accountRequestDTO,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new com.example.moneymarket.exception.BusinessException(
                    bindingResult.getAllErrors().get(0).getDefaultMessage());
        }
        
        CustomerAccountResponseDTO createdAccount = customerAccountService.createAccount(accountRequestDTO);
        return new ResponseEntity<>(createdAccount, HttpStatus.CREATED);
    }

    /**
     * Update an existing customer account
     * 
     * @param accountNo The account number
     * @param accountRequestDTO The account data
     * @param bindingResult Validation result
     * @return The updated account
     */
    @PutMapping("/{accountNo}")
    public ResponseEntity<CustomerAccountResponseDTO> updateAccount(
            @PathVariable String accountNo,
            @Valid @RequestBody CustomerAccountRequestDTO accountRequestDTO,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new com.example.moneymarket.exception.BusinessException(
                    bindingResult.getAllErrors().get(0).getDefaultMessage());
        }
        
        CustomerAccountResponseDTO updatedAccount = customerAccountService.updateAccount(accountNo, accountRequestDTO);
        return ResponseEntity.ok(updatedAccount);
    }

    /**
     * Get a customer account by account number
     * 
     * @param accountNo The account number
     * @return The account
     */
    @GetMapping("/{accountNo}")
    public ResponseEntity<CustomerAccountResponseDTO> getAccount(@PathVariable String accountNo) {
        CustomerAccountResponseDTO account = customerAccountService.getAccount(accountNo);
        return ResponseEntity.ok(account);
    }

    /**
     * Get all customer accounts with pagination
     * 
     * @param pageable The pagination information
     * @return Page of accounts
     */
    @GetMapping
    public ResponseEntity<Page<CustomerAccountResponseDTO>> getAllAccounts(Pageable pageable) {
        Page<CustomerAccountResponseDTO> accounts = customerAccountService.getAllAccounts(pageable);
        return ResponseEntity.ok(accounts);
    }

    /**
     * Close a customer account
     * 
     * @param accountNo The account number
     * @return The closed account
     */
    @PostMapping("/{accountNo}/close")
    public ResponseEntity<CustomerAccountResponseDTO> closeAccount(@PathVariable String accountNo) {
        CustomerAccountResponseDTO closedAccount = customerAccountService.closeAccount(accountNo);
        return ResponseEntity.ok(closedAccount);
    }
}
