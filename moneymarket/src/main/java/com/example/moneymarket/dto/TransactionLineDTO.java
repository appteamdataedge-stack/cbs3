package com.example.moneymarket.dto;

import com.example.moneymarket.entity.TranTable.DrCrFlag;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionLineDTO {

    @NotBlank(message = "Account Number is mandatory")
    @Size(max = 20, message = "Account Number cannot exceed 20 characters")
    private String accountNo;

    @NotNull(message = "Debit/Credit Flag is mandatory")
    private DrCrFlag drCrFlag;

    @NotNull(message = "Transaction Currency is mandatory")
    @Size(min = 3, max = 3, message = "Transaction Currency must be a 3-character code")
    private String tranCcy;

    @NotNull(message = "FCY Amount is mandatory")
    @DecimalMin(value = "0.01", message = "FCY Amount must be greater than zero")
    @Digits(integer = 18, fraction = 2, message = "FCY Amount can have at most 18 digits in total with 2 decimal places")
    private BigDecimal fcyAmt;

    @NotNull(message = "Exchange Rate is mandatory")
    @DecimalMin(value = "0.0001", message = "Exchange Rate must be greater than zero")
    @Digits(integer = 6, fraction = 4, message = "Exchange Rate can have at most 6 digits in total with 4 decimal places")
    private BigDecimal exchangeRate;

    @NotNull(message = "LCY Amount is mandatory")
    @DecimalMin(value = "0.01", message = "LCY Amount must be greater than zero")
    @Digits(integer = 18, fraction = 2, message = "LCY Amount can have at most 18 digits in total with 2 decimal places")
    private BigDecimal lcyAmt;

    @Size(max = 50, message = "UDF1 cannot exceed 50 characters")
    private String udf1;
}
