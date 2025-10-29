package com.example.moneymarket.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "OF_Acct_Master")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OFAcctMaster {

    @Id
    @Column(name = "Account_No", length = 13)
    private String accountNo;

    @ManyToOne
    @JoinColumn(name = "Sub_Product_Id", nullable = false)
    private SubProdMaster subProduct;

    @Column(name = "GL_Num", nullable = false, length = 20)
    private String glNum;

    @Column(name = "Acct_Name", nullable = false, length = 100)
    private String acctName;

    @Column(name = "Date_Opening", nullable = false)
    private LocalDate dateOpening;

    @Column(name = "Date_Closure")
    private LocalDate dateClosure;

    @Column(name = "Branch_Code", nullable = false, length = 10)
    private String branchCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "Account_Status", nullable = false)
    private AccountStatus accountStatus;

    @Column(name = "Reconciliation_Required", nullable = false)
    private Boolean reconciliationRequired;

    public enum AccountStatus {
        Active, Inactive, Closed
    }
}
