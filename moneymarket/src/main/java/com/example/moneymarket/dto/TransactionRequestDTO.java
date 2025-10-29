package com.example.moneymarket.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequestDTO {

    @NotNull(message = "Value Date is mandatory")
    private LocalDate valueDate;

    @Size(max = 100, message = "Narration cannot exceed 100 characters")
    private String narration;

    @NotEmpty(message = "Transaction must have at least one line")
    @Valid
    private List<TransactionLineDTO> lines;
}
