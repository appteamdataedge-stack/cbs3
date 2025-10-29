package com.example.moneymarket.dto;

import com.example.moneymarket.entity.OFAcctMaster.AccountStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfficeAccountRequestDTO {

    @NotNull(message = "Sub-Product ID is mandatory")
    private Integer subProductId;

    @NotBlank(message = "Account Name is mandatory")
    @Size(max = 100, message = "Account Name cannot exceed 100 characters")
    private String acctName;

    @NotNull(message = "Date of Opening is mandatory")
    private LocalDate dateOpening;

    private LocalDate dateClosure;

    @NotBlank(message = "Branch Code is mandatory")
    @Size(max = 10, message = "Branch Code cannot exceed 10 characters")
    private String branchCode;

    @NotNull(message = "Account Status is mandatory")
    private AccountStatus accountStatus;

    @NotNull(message = "Reconciliation Required flag is mandatory")
    private Boolean reconciliationRequired;
}
