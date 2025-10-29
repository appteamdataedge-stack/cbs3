package com.example.moneymarket.dto;

import com.example.moneymarket.entity.OFAcctMaster.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfficeAccountResponseDTO {

    private String accountNo;
    private Integer subProductId;
    private String subProductName;
    private String glNum;
    private String acctName;
    private LocalDate dateOpening;
    private LocalDate dateClosure;
    private String branchCode;
    private AccountStatus accountStatus;
    private Boolean reconciliationRequired;
}
