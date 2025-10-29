package com.example.moneymarket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for account balance information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountBalanceDTO {
    
    private String accountNo;
    private String accountName;
    private BigDecimal availableBalance; // Previous day opening balance
    private BigDecimal currentBalance;   // Current day balance from acct_bal
    private BigDecimal todayDebits;      // Current day debit transactions
    private BigDecimal todayCredits;     // Current day credit transactions
    private BigDecimal computedBalance;  // Previous day opening + current day credits - current day debits (REAL-TIME BALANCE)
    private BigDecimal interestAccrued;  // Latest closing balance from acct_bal_accrual table
}

