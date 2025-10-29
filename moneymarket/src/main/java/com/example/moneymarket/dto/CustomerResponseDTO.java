package com.example.moneymarket.dto;

import com.example.moneymarket.entity.CustMaster.CustomerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponseDTO {

    private Integer custId;
    private String extCustId;
    private CustomerType custType;
    private String firstName;
    private String lastName;
    private String tradeName;
    private String address1;
    private String mobile;
    private String branchCode;
    private String makerId;
    private LocalDate entryDate;
    private LocalTime entryTime;
    private String verifierId;
    private LocalDate verificationDate;
    private LocalTime verificationTime;
    private boolean verified;
    
    // Message for UI display (e.g., "Customer ID xxxxxxxx created")
    private String message;
}
