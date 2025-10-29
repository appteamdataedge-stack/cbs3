package com.example.moneymarket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDTO {

    private Integer productId;
    private String productCode;
    private String productName;
    private String cumGLNum;
    private Boolean customerProductFlag;
    private Boolean interestBearingFlag;
    private String dealOrRunning;
    private String currency;
    private String makerId;
    private LocalDate entryDate;
    private LocalTime entryTime;
    private String verifierId;
    private LocalDate verificationDate;
    private LocalTime verificationTime;
    private boolean verified;
    private List<SubProductResponseDTO> subProducts;
}
