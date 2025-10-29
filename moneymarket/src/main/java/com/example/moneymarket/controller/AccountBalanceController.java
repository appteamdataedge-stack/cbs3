package com.example.moneymarket.controller;

import com.example.moneymarket.dto.AccountBalanceDTO;
import com.example.moneymarket.service.BalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for account balance operations
 */
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountBalanceController {

    private final BalanceService balanceService;

    /**
     * Get computed available balance for an account
     * Returns: Available Balance + Credits - Debits from today's transactions
     * 
     * @param accountNo The account number
     * @return The computed balance information
     */
    @GetMapping("/{accountNo}/balance")
    public ResponseEntity<AccountBalanceDTO> getAccountBalance(@PathVariable String accountNo) {
        AccountBalanceDTO balance = balanceService.getComputedAccountBalance(accountNo);
        return ResponseEntity.ok(balance);
    }
}

