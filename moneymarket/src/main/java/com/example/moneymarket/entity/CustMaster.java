package com.example.moneymarket.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "Cust_Master")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustMaster {

    @Id
    @Column(name = "Cust_Id")
    private Integer custId;

    @Column(name = "Ext_Cust_Id", nullable = false, length = 20)
    private String extCustId;

    @Enumerated(EnumType.STRING)
    @Column(name = "Cust_Type", nullable = false)
    private CustomerType custType;

    @Column(name = "First_Name", length = 50)
    private String firstName;

    @Column(name = "Last_Name", length = 50)
    private String lastName;

    @Column(name = "Trade_Name", length = 100)
    private String tradeName;

    @Column(name = "Address_1", length = 200)
    private String address1;

    @Column(name = "Mobile", length = 15)
    private String mobile;
    
    @Column(name = "Branch_Code", length = 10)
    private String branchCode;

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

    public enum CustomerType {
        Individual, Corporate, Bank
    }
}
