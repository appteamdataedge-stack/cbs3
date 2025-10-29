package com.example.moneymarket.dto;

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
public class InterestRateResponseDTO {

    private String inttCode;
    private BigDecimal inttRate;
    private LocalDate inttEffctvDate;
}
