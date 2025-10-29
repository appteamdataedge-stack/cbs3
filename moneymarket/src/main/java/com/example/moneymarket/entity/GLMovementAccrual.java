package com.example.moneymarket.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "GL_Movement_Accrual")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GLMovementAccrual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Movement_Id")
    private Long movementId;

    @ManyToOne
    @JoinColumn(name = "Accr_Tran_Id", nullable = false)
    private InttAccrTran accrual;

    // FIX: Use LAZY fetch to prevent automatic joining of GLSetup during batch queries
    // GL_Num is not unique across different accrual dates, which can cause
    // Hibernate duplicate-row assertion errors when querying by GL_Num
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GL_Num", nullable = false)
    private GLSetup glSetup;

    @Enumerated(EnumType.STRING)
    @Column(name = "Dr_Cr_Flag", nullable = false)
    private TranTable.DrCrFlag drCrFlag;

    @Column(name = "Accrual_Date", nullable = false)
    private LocalDate accrualDate;

    @Column(name = "Amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false)
    private InttAccrTran.AccrualStatus status;

    // Added as per BRD PTTP02V1.0
    @Column(name = "Tran_date", nullable = true)
    private LocalDate tranDate;

    // Added as per BRD PTTP02V1.0
    @Column(name = "Tran_Id", length = 20, nullable = true)
    private String tranId;

    // Added as per BRD PTTP02V1.0
    @Column(name = "Tran_Ccy", length = 3, nullable = true)
    private String tranCcy;

    // Added as per BRD PTTP02V1.0
    @Column(name = "FCY_Amt", precision = 20, scale = 2, nullable = true)
    private BigDecimal fcyAmt;

    // Added as per BRD PTTP02V1.0
    @Column(name = "Exchange_Rate", precision = 10, scale = 4, nullable = true)
    private BigDecimal exchangeRate;

    // Added as per BRD PTTP02V1.0
    @Column(name = "LCY_Amt", precision = 20, scale = 2, nullable = true)
    private BigDecimal lcyAmt;

    // Added as per BRD PTTP02V1.0
    @Column(name = "Narration", length = 100, nullable = true)
    private String narration;
}
