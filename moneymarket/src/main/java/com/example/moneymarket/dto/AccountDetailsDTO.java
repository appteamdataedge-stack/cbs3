package com.example.moneymarket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for account details used in Statement of Accounts
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDetailsDTO {
    private String accountNo;
    private String accountName;
    private String customerName; // Nullable for office accounts
    private String accountType; // "Customer" or "Office"
    private String glNum;
    private String productName;
    private String branchId;
}

