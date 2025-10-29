package com.example.moneymarket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for GL DR/CR Summation results from native queries
 * Used to avoid Hibernate duplicate row issues when GL_Num is not unique
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GLDrCrSummationDTO {
    private String glNum;
    private BigDecimal totalDr;
    private BigDecimal totalCr;
    
    /**
     * Constructor for native query result mapping
     * Handles null values from database
     */
    public GLDrCrSummationDTO(String glNum, Object totalDr, Object totalCr) {
        this.glNum = glNum;
        this.totalDr = totalDr != null ? new BigDecimal(totalDr.toString()) : BigDecimal.ZERO;
        this.totalCr = totalCr != null ? new BigDecimal(totalCr.toString()) : BigDecimal.ZERO;
    }
}

