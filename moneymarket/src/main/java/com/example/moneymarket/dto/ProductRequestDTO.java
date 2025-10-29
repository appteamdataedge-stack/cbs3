package com.example.moneymarket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequestDTO {

    @NotBlank(message = "Product Code is mandatory")
    @Size(max = 10, message = "Product Code cannot exceed 10 characters")
    private String productCode;

    @NotBlank(message = "Product Name is mandatory")
    @Size(max = 50, message = "Product Name cannot exceed 50 characters")
    private String productName;

    @NotBlank(message = "GL Number is mandatory")
    @Size(max = 20, message = "GL Number cannot exceed 20 characters")
    private String cumGLNum;

    // Flags per BRD
    private Boolean customerProductFlag;
    private Boolean interestBearingFlag;
    private String dealOrRunning;
    private String currency;

    @NotBlank(message = "Maker ID is mandatory")
    @Size(max = 20, message = "Maker ID cannot exceed 20 characters")
    private String makerId;
}
