package com.example.moneymarket.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "Prod_Master")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProdMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Product_Id")
    private Integer productId;

    @Column(name = "Product_Code", nullable = false, unique = true, length = 10)
    private String productCode;

    @Column(name = "Product_Name", nullable = false, length = 50)
    private String productName;

    @Column(name = "Cum_GL_Num", nullable = false, length = 20)
    private String cumGLNum;

    // Flags per BRD
    @Column(name = "Customer_Product_Flag")
    private Boolean customerProductFlag;

    @Column(name = "Interest_Bearing_Flag")
    private Boolean interestBearingFlag;

    @Column(name = "Deal_Or_Running", length = 10)
    private String dealOrRunning;

    @Column(name = "Currency", length = 3)
    private String currency;

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

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<SubProdMaster> subProducts;
}
