package com.example.moneymarket.dto;

import com.example.moneymarket.entity.CustAcctMaster.AccountStatus;
import jakarta.validation.constraints.*;
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
public class CustomerAccountRequestDTO {

    @NotNull(message = "Sub-Product ID is mandatory")
    private Integer subProductId;

    @NotNull(message = "Customer ID is mandatory")
    private Integer custId;

    @NotBlank(message = "Customer Name is mandatory")
    @Size(max = 100, message = "Customer Name cannot exceed 100 characters")
    private String custName;

    @NotBlank(message = "Account Name is mandatory")
    @Size(max = 100, message = "Account Name cannot exceed 100 characters")
    private String acctName;

    @NotNull(message = "Date of Opening is mandatory")
    private LocalDate dateOpening;

    @Min(value = 1, message = "Tenor must be at least 1 day")
    @Max(value = 999, message = "Tenor cannot exceed 999 days")
    private Integer tenor;

    private LocalDate dateMaturity;

    private LocalDate dateClosure;

    @NotBlank(message = "Branch Code is mandatory")
    @Size(max = 10, message = "Branch Code cannot exceed 10 characters")
    private String branchCode;

    @NotNull(message = "Account Status is mandatory")
    private AccountStatus accountStatus;

    @Min(value = 0, message = "Loan limit cannot be negative")
    private BigDecimal loanLimit;
}
