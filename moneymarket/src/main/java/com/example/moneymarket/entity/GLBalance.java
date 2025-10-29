package com.example.moneymarket.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * GL Balance Entity
 *
 * SCHEMA CHANGE (2025-10-23):
 * - Changed from composite PK (GL_Num, Tran_date) to auto-increment Id
 * - Added unique constraint on (GL_Num, Tran_date) to maintain data integrity
 * - This simplifies JPA queries and eliminates composite key complexity
 */
@Entity
@Table(name = "GL_Balance",
       uniqueConstraints = @UniqueConstraint(
           name = "uq_gl_balance_gl_num_tran_date",
           columnNames = {"GL_Num", "Tran_date"}
       ))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GLBalance {

    /**
     * Auto-increment primary key
     * This replaces the previous composite PK (GL_Num, Tran_date)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;

    /**
     * GL Account Number
     * Part of unique constraint with Tran_date
     */
    @Column(name = "GL_Num", length = 9, nullable = false)
    private String glNum;

    /**
     * Transaction Date
     * Part of unique constraint with GL_Num
     */
    @Column(name = "Tran_date", nullable = false)
    private LocalDate tranDate;

    /**
     * Reference to GL Setup (Chart of Accounts)
     * Many GL balance records can belong to one GL account
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GL_Num", insertable = false, updatable = false)
    private GLSetup glSetup;

    @Column(name = "Current_Balance", nullable = false, precision = 20, scale = 2)
    private BigDecimal currentBalance;

    @Column(name = "Last_Updated", nullable = false)
    private LocalDateTime lastUpdated;

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

    /**
     * REMOVED: Composite Primary Key class (no longer needed)
     *
     * Previous Implementation:
     * - Used @IdClass(GLBalance.GLBalanceId.class) with composite PK
     * - Required GLBalanceId inner class with (glNum, tranDate)
     *
     * New Implementation:
     * - Uses auto-increment Long id as primary key
     * - Unique constraint on (GL_Num, Tran_date) maintains data integrity
     */
}
