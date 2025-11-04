package com.example.moneymarket.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "Sub_Prod_Master")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubProdMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Sub_Product_Id")
    private Integer subProductId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Product_Id", nullable = false)
    private ProdMaster product;

    @Column(name = "Sub_Product_Code", nullable = false, unique = true, length = 10)
    private String subProductCode;

    @Column(name = "Sub_Product_Name", nullable = false, length = 50)
    private String subProductName;

    @Column(name = "Intt_Code", length = 20)
    private String inttCode;

    @Column(name = "Cum_GL_Num", nullable = false, length = 10)
    private String cumGLNum;

    @Column(name = "Ext_GL_Num", length = 10)
    private String extGLNum;

    // Interest-related fields when product is customer+interest bearing
    @Column(name = "Interest_Increment", precision = 10, scale = 4)
    private java.math.BigDecimal interestIncrement;

    @Column(name = "Effective_Interest_Rate", precision = 10, scale = 4)
    private java.math.BigDecimal effectiveInterestRate;

    /**
     * Consolidated GL field for Receivable/Expenditure
     * - For LIABILITY products (GL starts with '1'): stores EXPENDITURE GL (debit side)
     * - For ASSET products (GL starts with '2'): stores RECEIVABLE GL (debit side)
     */
    @Column(name = "interest_receivable_expenditure_gl_num", length = 20)
    private String interestReceivableExpenditureGLNum;

    /**
     * Consolidated GL field for Income/Payable
     * - For LIABILITY products (GL starts with '1'): stores PAYABLE GL (credit side)
     * - For ASSET products (GL starts with '2'): stores INCOME GL (credit side)
     */
    @Column(name = "interest_income_payable_gl_num", length = 20)
    private String interestIncomePayableGLNum;

    @Enumerated(EnumType.STRING)
    @Column(name = "Sub_Product_Status", nullable = false)
    private SubProductStatus subProductStatus;

    @Column(name = "Maker_Id", nullable = false, length = 20)
    private String makerId;

    @Column(name = "Entry_Date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "Entry_Time", nullable = false)
    private LocalTime entryTime;

    @Column(name = "Verifier_Id", length = 20)
    private String verifierId;

    @Column(name = "Verification_Date")
    private LocalDate verificationDate;

    @Column(name = "Verification_Time")
    private LocalTime verificationTime;

    public enum SubProductStatus {
        Active, Inactive, Deactive
    }
}
