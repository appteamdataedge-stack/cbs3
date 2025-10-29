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
@Table(name = "Intt_Accr_Tran")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InttAccrTran {

    /**
     * Custom ID generation: S + YYYYMMDD + 9-digit-sequential + -row-suffix
     * Example: S20251020000000001-1, S20251020000000001-2
     * Generated programmatically in InterestAccrualService
     */
    @Id
    @Column(name = "Accr_Tran_Id", length = 20)
    private String accrTranId;

    @Column(name = "Account_No", nullable = false, length = 13)
    private String accountNo;

    @Column(name = "Accrual_Date", nullable = false)
    private LocalDate accrualDate;

    @Column(name = "Interest_Rate", nullable = false, precision = 10, scale = 4)
    private BigDecimal interestRate;

    @Column(name = "Amount", nullable = false, precision = 20, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false)
    private AccrualStatus status;

    @OneToMany(mappedBy = "accrual", cascade = CascadeType.ALL)
    private List<GLMovementAccrual> accrualMovements;

    // Added as per BRD PTTP02V1.0
    @Column(name = "Tran_Date", nullable = true)
    private LocalDate tranDate;

    // Added as per BRD PTTP02V1.0
    @Column(name = "Value_Date", nullable = true)
    private LocalDate valueDate;

    // Added as per BRD PTTP02V1.0
    @Enumerated(EnumType.STRING)
    @Column(name = "Dr_Cr_Flag", nullable = true)
    private TranTable.DrCrFlag drCrFlag;

    // Added as per BRD PTTP02V1.0
    @Enumerated(EnumType.STRING)
    @Column(name = "Tran_Status", nullable = true)
    private TranTable.TranStatus tranStatus;

    // Added as per BRD PTTP02V1.0
    @Column(name = "GL_Account_No", length = 20, nullable = true)
    private String glAccountNo;

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

    // Added as per BRD PTTP02V1.0
    @Column(name = "UDF1", length = 50, nullable = true)
    private String udf1;

    public enum AccrualStatus {
        Pending, Posted, Verified
    }
}
