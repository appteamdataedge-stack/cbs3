package com.example.moneymarket.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "Acct_Bal_Accrual")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcctBalAccrual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Accr_Bal_Id")
    private Long accrBalId;

    @ManyToOne
    @JoinColumn(name = "Account_No", nullable = false)
    private CustAcctMaster account;

    @Column(name = "GL_Num", length = 9)
    private String glNum;

    @Column(name = "Accrual_Date", nullable = false)
    private LocalDate accrualDate;

    @Column(name = "Interest_Amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal interestAmount;

    // Added as per BRD PTTP02V1.0
    @Column(name = "Tran_date", nullable = true)
    private LocalDate tranDate;

    // Added as per BRD PTTP02V1.0
    @Column(name = "Opening_Bal", precision = 20, scale = 2, nullable = true)
    private BigDecimal openingBal;

    // Added as per BRD PTTP02V1.0
    @Column(name = "DR_Summation", precision = 20, scale = 2, nullable = true)
    private BigDecimal drSummation;

    // Added as per BRD PTTP02V1.0
    @Column(name = "CR_Summation", precision = 20, scale = 2, nullable = true)
    private BigDecimal crSummation;

    // Added as per BRD PTTP02V1.0
    @Column(name = "Closing_Bal", precision = 20, scale = 2, nullable = true)
    private BigDecimal closingBal;
}
