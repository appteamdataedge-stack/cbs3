package com.example.moneymarket.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Entity class for Transaction History Account (TXN_HIST_ACCT)
 * Captures all account transactions in real-time for Statement of Accounts generation
 */
@Entity
@Table(name = "txn_hist_acct", indexes = {
    @Index(name = "idx_acc_no", columnList = "ACC_No"),
    @Index(name = "idx_tran_date", columnList = "TRAN_DATE"),
    @Index(name = "idx_acc_tran_date", columnList = "ACC_No, TRAN_DATE"),
    @Index(name = "idx_tran_id", columnList = "TRAN_ID")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TxnHistAcct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Hist_ID")
    private Long histId;

    @Column(name = "Branch_ID", length = 10, nullable = false)
    private String branchId;

    @Column(name = "ACC_No", length = 13, nullable = false)
    private String accNo;

    @Column(name = "TRAN_ID", length = 20, nullable = false)
    private String tranId;

    @Column(name = "TRAN_DATE", nullable = false)
    private LocalDate tranDate;

    @Column(name = "VALUE_DATE", nullable = false)
    private LocalDate valueDate;

    @Column(name = "TRAN_SL_NO", nullable = false)
    private Integer tranSlNo;

    @Column(name = "NARRATION", length = 100)
    private String narration;

    @Enumerated(EnumType.STRING)
    @Column(name = "TRAN_TYPE", nullable = false, columnDefinition = "ENUM('D', 'C')")
    private TransactionType tranType;

    @Column(name = "TRAN_AMT", precision = 20, scale = 2, nullable = false)
    private BigDecimal tranAmt;

    @Column(name = "Opening_Balance", precision = 20, scale = 2, nullable = false)
    private BigDecimal openingBalance;

    @Column(name = "BALANCE_AFTER_TRAN", precision = 20, scale = 2, nullable = false)
    private BigDecimal balanceAfterTran;

    @Column(name = "ENTRY_USER_ID", length = 20, nullable = false)
    private String entryUserId;

    @Column(name = "AUTH_USER_ID", length = 20)
    private String authUserId;

    @Column(name = "CURRENCY_CODE", length = 3)
    private String currencyCode = "BDT";

    @Column(name = "GL_Num", length = 9)
    private String glNum;

    @Column(name = "RCRE_DATE", nullable = false)
    private LocalDate rcreDate;

    @Column(name = "RCRE_TIME", nullable = false)
    private LocalTime rcreTime;

    /**
     * Optional: ManyToOne relationship to TranTable entity
     * Uncomment if you want to navigate from TxnHistAcct to TranTable
     */
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "TRAN_ID", referencedColumnName = "Tran_Id", insertable = false, updatable = false)
    // private TranTable transaction;

    /**
     * Enum for Transaction Type
     */
    public enum TransactionType {
        D, // Debit
        C  // Credit
    }
}

