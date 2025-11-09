package com.example.moneymarket.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity for logging value-dated transactions
 * Tracks delta interest calculations and posting status for past/future-dated transactions
 */
@Entity
@Table(name = "Tran_Value_Date_Log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranValueDateLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Log_Id")
    private Long logId;

    @Column(name = "Tran_Id", nullable = false, length = 20)
    private String tranId;

    @Column(name = "Value_Date", nullable = false)
    private LocalDate valueDate;

    @Column(name = "Days_Difference", nullable = false)
    private Integer daysDifference;

    @Column(name = "Delta_Interest_Amt", precision = 20, scale = 4)
    private BigDecimal deltaInterestAmt;

    @Column(name = "Adjustment_Posted_Flag", length = 1)
    private String adjustmentPostedFlag;

    @Column(name = "Created_Timestamp", nullable = false)
    private LocalDateTime createdTimestamp;
}
