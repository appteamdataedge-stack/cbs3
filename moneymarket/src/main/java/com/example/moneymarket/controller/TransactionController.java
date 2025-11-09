package com.example.moneymarket.controller;

import com.example.moneymarket.dto.TransactionRequestDTO;
import com.example.moneymarket.dto.TransactionResponseDTO;
import com.example.moneymarket.service.SystemDateService;
import com.example.moneymarket.service.TransactionService;
import com.example.moneymarket.service.UnifiedAccountService;
import com.example.moneymarket.validation.TransactionValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for transaction operations
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionValidator transactionValidator;
    private final UnifiedAccountService unifiedAccountService;
    private final SystemDateService systemDateService;

    /**
     * Initialize validator for transaction request
     * 
     * @param binder The WebDataBinder
     */
    @InitBinder("transactionRequestDTO")
    public void initBinder(WebDataBinder binder) {
        binder.addValidators(transactionValidator);
    }

    /**
     * Get all transactions with pagination
     * 
     * @param page The page number (default 0)
     * @param size The page size (default 10)
     * @param sort The sort field (optional)
     * @return Page of transactions
     */
    @GetMapping
    public ResponseEntity<Page<TransactionResponseDTO>> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort) {
        
        Pageable pageable;
        if (sort != null && !sort.isEmpty()) {
            String[] sortParams = sort.split(",");
            String field = sortParams[0];
            Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("desc") 
                    ? Sort.Direction.DESC 
                    : Sort.Direction.ASC;
            pageable = PageRequest.of(page, size, Sort.by(direction, field));
        } else {
            pageable = PageRequest.of(page, size);
        }
        
        Page<TransactionResponseDTO> transactions = transactionService.getAllTransactions(pageable);
        return ResponseEntity.ok(transactions);
    }

    /**
     * Get the current system date used for transactions
     *
     * @return System date in ISO format
     */
    @GetMapping("/default-value-date")
    public ResponseEntity<Map<String, String>> getSystemDate() {
        Map<String, String> response = new HashMap<>();
        response.put("systemDate", systemDateService.getSystemDate().toString());
        return ResponseEntity.ok(response);
    }

    /**
     * Create a new transaction
     * 
     * @param transactionRequestDTO The transaction data
     * @param bindingResult Validation result
     * @return The created transaction
     */
    @PostMapping("/entry")
    public ResponseEntity<TransactionResponseDTO> createTransaction(
            @Valid @RequestBody TransactionRequestDTO transactionRequestDTO,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new com.example.moneymarket.exception.BusinessException(
                    bindingResult.getAllErrors().get(0).getDefaultMessage());
        }
        
        TransactionResponseDTO createdTransaction = transactionService.createTransaction(transactionRequestDTO);
        return new ResponseEntity<>(createdTransaction, HttpStatus.CREATED);
    }

    /**
     * Get a transaction by ID
     * 
     * @param tranId The transaction ID
     * @return The transaction
     */
    @GetMapping("/{tranId:T[0-9\\-]+}")
    public ResponseEntity<TransactionResponseDTO> getTransaction(@PathVariable String tranId) {
        TransactionResponseDTO transaction = transactionService.getTransaction(tranId);
        return ResponseEntity.ok(transaction);
    }

    /**
     * Post a transaction (Checker approves Maker's entry)
     * Moves transaction from Entry to Posted status and updates balances
     * 
     * @param tranId The transaction ID
     * @return The posted transaction
     */
    @PostMapping("/{tranId:T[0-9\\-]+}/post")
    public ResponseEntity<TransactionResponseDTO> postTransaction(@PathVariable String tranId) {
        TransactionResponseDTO postedTransaction = transactionService.postTransaction(tranId);
        return ResponseEntity.ok(postedTransaction);
    }

    /**
     * Verify a transaction (Final approval)
     * Moves transaction from Posted to Verified status
     * 
     * @param tranId The transaction ID
     * @return The verified transaction
     */
    @PostMapping("/{tranId:T[0-9\\-]+}/verify")
    public ResponseEntity<TransactionResponseDTO> verifyTransaction(@PathVariable String tranId) {
        TransactionResponseDTO verifiedTransaction = transactionService.verifyTransaction(tranId);
        return ResponseEntity.ok(verifiedTransaction);
    }

    /**
     * Check if an account is an overdraft account
     * 
     * @param accountNo The account number
     * @return Account information including overdraft status
     */
    @GetMapping("/account/{accountNo}/overdraft-status")
    public ResponseEntity<Map<String, Object>> getAccountOverdraftStatus(@PathVariable String accountNo) {
        try {
            UnifiedAccountService.AccountInfo accountInfo = unifiedAccountService.getAccountInfo(accountNo);
            Map<String, Object> response = new HashMap<>();
            response.put("accountNo", accountInfo.getAccountNo());
            response.put("accountName", accountInfo.getAccountName());
            response.put("glNum", accountInfo.getGlNum());
            response.put("isOverdraftAccount", accountInfo.isOverdraftAccount());
            response.put("isCustomerAccount", accountInfo.isCustomerAccount());
            response.put("isAssetAccount", accountInfo.isAssetAccount());
            response.put("isLiabilityAccount", accountInfo.isLiabilityAccount());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Account not found: " + accountNo);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    /**
     * Reverse a transaction
     * Creates opposite entries to reverse the original transaction
     * 
     * @param tranId The transaction ID to reverse
     * @param reason The reason for reversal
     * @return The reversal transaction
     */
    @PostMapping("/{tranId:T[0-9\\-]+}/reverse")
    public ResponseEntity<TransactionResponseDTO> reverseTransaction(
            @PathVariable String tranId,
            @RequestParam(required = false, defaultValue = "Manual reversal") String reason) {
        TransactionResponseDTO reversalTransaction = transactionService.reverseTransaction(tranId, reason);
        return ResponseEntity.ok(reversalTransaction);
    }
}
