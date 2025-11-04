package com.example.moneymarket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for account options in dropdown lists
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountOptionDTO {
    private String accountNo;
    private String accountName;
    private String accountType; // "Customer" or "Office"
}

