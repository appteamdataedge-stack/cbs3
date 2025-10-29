package com.example.moneymarket.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "GL_Movement")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GLMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Movement_Id")
    private Long movementId;

    @ManyToOne
    @JoinColumn(name = "Tran_Id", nullable = false)
    private TranTable transaction;

    // FIX: Use LAZY fetch to prevent automatic joining of GLSetup during batch queries
    // GL_Num is not unique across different transaction dates, which can cause
    // Hibernate duplicate-row assertion errors when querying by GL_Num
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GL_Num", nullable = false)
    private GLSetup glSetup;

    @Enumerated(EnumType.STRING)
    @Column(name = "Dr_Cr_Flag", nullable = false)
    private TranTable.DrCrFlag drCrFlag;

    @Column(name = "Tran_Date", nullable = false)
    private LocalDate tranDate;

    @Column(name = "Value_Date", nullable = false)
    private LocalDate valueDate;

    @Column(name = "Amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal amount;

    @Column(name = "Balance_After", nullable = false, precision = 20, scale = 2)
    private BigDecimal balanceAfter;

    // Added as per BRD PTTP02V1.0
    @Column(name = "Tran_Ccy", length = 3, nullable = true)
    private String tranCcy;

    // Added as per BRD PTTP02V1.0
    @Column(name = "FCY_Amt", precision = 20, scale = 2, nullable = true)
    private BigDecimal fcyAmt;

    // Added as per BRD PTTP02V1.0
    @Column(name = "LCY_Amt", precision = 20, scale = 2, nullable = true)
    private BigDecimal lcyAmt;

    // Added as per BRD PTTP02V1.0
    @Column(name = "Narration", length = 100, nullable = true)
    private String narration;
}
