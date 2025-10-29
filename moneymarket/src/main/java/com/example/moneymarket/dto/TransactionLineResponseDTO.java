package com.example.moneymarket.dto;

import com.example.moneymarket.entity.TranTable.DrCrFlag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionLineResponseDTO {

    private String tranId;
    private String accountNo;
    private String accountName;
    private DrCrFlag drCrFlag;
    private String tranCcy;
    private BigDecimal fcyAmt;
    private BigDecimal exchangeRate;
    private BigDecimal lcyAmt;
    private String udf1;
}
