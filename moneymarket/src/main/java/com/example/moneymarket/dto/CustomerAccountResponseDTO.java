package com.example.moneymarket.dto;

import com.example.moneymarket.entity.CustAcctMaster.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAccountResponseDTO {

    private String accountNo;
    private Integer subProductId;
    private String subProductName;
    private String glNum;
    private Integer custId;
    private String custName;
    private String acctName;
    private LocalDate dateOpening;
    private Integer tenor;
    private LocalDate dateMaturity;
    private LocalDate dateClosure;
    private String branchCode;
    private AccountStatus accountStatus;
    private BigDecimal currentBalance;      // Static balance from acct_bal table
    private BigDecimal availableBalance;    // Previous day opening balance (for Liability) or includes loan limit (for Asset)
    private BigDecimal computedBalance;     // Real-time computed balance (Prev Day + Credits - Debits)
    private BigDecimal interestAccrued;     // Latest closing balance from acct_bal_accrual
    private BigDecimal loanLimit;           // Loan/Limit Amount for Asset-side accounts (GL starting with "2")
    
    // Message for UI display (e.g., confirmation messages)
    private String message;
}
