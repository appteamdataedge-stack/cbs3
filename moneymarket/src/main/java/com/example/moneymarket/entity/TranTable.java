package com.example.moneymarket.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "Tran_Table")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranTable {

    @Id
    @Column(name = "Tran_Id", length = 20)
    private String tranId;

    @Column(name = "Tran_Date", nullable = false)
    private LocalDate tranDate;

    @Column(name = "Value_Date", nullable = false)
    private LocalDate valueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "Dr_Cr_Flag", nullable = false)
    private DrCrFlag drCrFlag;

    @Enumerated(EnumType.STRING)
    @Column(name = "Tran_Status", nullable = false)
    private TranStatus tranStatus;

    @Column(name = "Account_No", nullable = false, length = 20)
    private String accountNo;

    @Column(name = "Tran_Ccy", nullable = false, length = 3)
    private String tranCcy;

    @Column(name = "FCY_Amt", nullable = false, precision = 20, scale = 2)
    private BigDecimal fcyAmt;

    @Column(name = "Exchange_Rate", nullable = false, precision = 10, scale = 4)
    private BigDecimal exchangeRate;

    @Column(name = "LCY_Amt", nullable = false, precision = 20, scale = 2)
    private BigDecimal lcyAmt;
    
    @Column(name = "Debit_Amount", precision = 20, scale = 2)
    private BigDecimal debitAmount;
    
    @Column(name = "Credit_Amount", precision = 20, scale = 2)
    private BigDecimal creditAmount;

    @Column(name = "Narration", length = 100)
    private String narration;

    @Column(name = "UDF1", length = 50)
    private String udf1;

    @Column(name = "Pointing_Id")
    private Integer pointingId;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL)
    private List<GLMovement> glMovements;

    public enum DrCrFlag {
        D, C
    }

    public enum TranStatus {
        Entry, Posted, Verified, Future
    }
}
