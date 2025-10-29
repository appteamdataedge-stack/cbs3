package com.example.moneymarket.controller;

import com.example.moneymarket.dto.OfficeAccountRequestDTO;
import com.example.moneymarket.dto.OfficeAccountResponseDTO;
import com.example.moneymarket.service.OfficeAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for office account operations
 */
@RestController
@RequestMapping("/api/accounts/office")
@RequiredArgsConstructor
public class OfficeAccountController {

    private final OfficeAccountService officeAccountService;

    /**
     * Create a new office account
     * 
     * @param accountRequestDTO The account data
     * @return The created account
     */
    @PostMapping
    public ResponseEntity<OfficeAccountResponseDTO> createAccount(
            @Valid @RequestBody OfficeAccountRequestDTO accountRequestDTO) {
        OfficeAccountResponseDTO createdAccount = officeAccountService.createAccount(accountRequestDTO);
        return new ResponseEntity<>(createdAccount, HttpStatus.CREATED);
    }

    /**
     * Update an existing office account
     * 
     * @param accountNo The account number
     * @param accountRequestDTO The account data
     * @return The updated account
     */
    @PutMapping("/{accountNo}")
    public ResponseEntity<OfficeAccountResponseDTO> updateAccount(
            @PathVariable String accountNo,
            @Valid @RequestBody OfficeAccountRequestDTO accountRequestDTO) {
        OfficeAccountResponseDTO updatedAccount = officeAccountService.updateAccount(accountNo, accountRequestDTO);
        return ResponseEntity.ok(updatedAccount);
    }

    /**
     * Get an office account by account number
     * 
     * @param accountNo The account number
     * @return The account
     */
    @GetMapping("/{accountNo}")
    public ResponseEntity<OfficeAccountResponseDTO> getAccount(@PathVariable String accountNo) {
        OfficeAccountResponseDTO account = officeAccountService.getAccount(accountNo);
        return ResponseEntity.ok(account);
    }

    /**
     * Get all office accounts with pagination
     * 
     * @param pageable The pagination information
     * @return Page of accounts
     */
    @GetMapping
    public ResponseEntity<Page<OfficeAccountResponseDTO>> getAllAccounts(Pageable pageable) {
        Page<OfficeAccountResponseDTO> accounts = officeAccountService.getAllAccounts(pageable);
        return ResponseEntity.ok(accounts);
    }

    /**
     * Close an office account
     * 
     * @param accountNo The account number
     * @return The closed account
     */
    @PostMapping("/{accountNo}/close")
    public ResponseEntity<OfficeAccountResponseDTO> closeAccount(@PathVariable String accountNo) {
        OfficeAccountResponseDTO closedAccount = officeAccountService.closeAccount(accountNo);
        return ResponseEntity.ok(closedAccount);
    }
}
