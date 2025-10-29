package com.example.moneymarket.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "Acct_Bal")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(AcctBalId.class)
public class AcctBal {

    @Id
    @Column(name = "Tran_Date", nullable = false)
    private LocalDate tranDate;

    @Id
    @Column(name = "Account_No", length = 13, nullable = false)
    private String accountNo;


    @Column(name = "Opening_Bal", precision = 20, scale = 2, nullable = true)
    private BigDecimal openingBal;

    @Column(name = "DR_Summation", precision = 20, scale = 2, nullable = true)
    private BigDecimal drSummation;

    @Column(name = "CR_Summation", precision = 20, scale = 2, nullable = true)
    private BigDecimal crSummation;

    @Column(name = "Closing_Bal", precision = 20, scale = 2, nullable = true)
    private BigDecimal closingBal;

    @Column(name = "Current_Balance", nullable = false, precision = 20, scale = 2)
    private BigDecimal currentBalance;

    @Column(name = "Available_Balance", nullable = false, precision = 20, scale = 2)
    private BigDecimal availableBalance;

    @Column(name = "Last_Updated", nullable = false)
    private LocalDateTime lastUpdated;
}
