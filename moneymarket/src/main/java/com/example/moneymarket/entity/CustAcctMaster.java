package com.example.moneymarket.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "Cust_Acct_Master")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustAcctMaster {

    @Id
    @Column(name = "Account_No", length = 13)
    private String accountNo;

    @ManyToOne
    @JoinColumn(name = "Sub_Product_Id", nullable = false)
    private SubProdMaster subProduct;

    @Column(name = "GL_Num", nullable = false, length = 20)
    private String glNum;

    @ManyToOne
    @JoinColumn(name = "Cust_Id", nullable = false)
    private CustMaster customer;

    @Column(name = "Cust_Name", length = 100)
    private String custName;

    @Column(name = "Acct_Name", nullable = false, length = 100)
    private String acctName;

    @Column(name = "Date_Opening", nullable = false)
    private LocalDate dateOpening;

    @Column(name = "Tenor")
    private Integer tenor;

    @Column(name = "Date_Maturity")
    private LocalDate dateMaturity;

    @Column(name = "Date_Closure")
    private LocalDate dateClosure;

    @Column(name = "Branch_Code", nullable = false, length = 10)
    private String branchCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "Account_Status", nullable = false)
    private AccountStatus accountStatus;

    @Column(name = "Loan_Limit", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal loanLimit = BigDecimal.ZERO;


    public enum AccountStatus {
        Active, Inactive, Closed, Dormant
    }
}
