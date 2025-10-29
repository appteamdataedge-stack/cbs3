package com.example.moneymarket.dto;

import com.example.moneymarket.entity.CustMaster.CustomerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRequestDTO {

    // External Customer ID is optional; when provided, must not exceed 20 chars
    @Size(max = 20, message = "External Customer ID cannot exceed 20 characters")
    private String extCustId;

    @NotNull(message = "Customer Type is mandatory")
    private CustomerType custType;

    @Size(max = 50, message = "First Name cannot exceed 50 characters")
    private String firstName;

    @Size(max = 50, message = "Last Name cannot exceed 50 characters")
    private String lastName;

    @Size(max = 100, message = "Trade Name cannot exceed 100 characters")
    private String tradeName;

    @Size(max = 200, message = "Address cannot exceed 200 characters")
    private String address1;

    @Pattern(regexp = "^[0-9]{1,15}$", message = "Mobile number must contain only digits and cannot exceed 15 digits")
    @Size(max = 15, message = "Mobile number cannot exceed 15 digits")
    private String mobile;
    
    @Size(max = 10, message = "Branch Code cannot exceed 10 characters")
    @Builder.Default
    private String branchCode = "001"; // Default branch code

    @NotBlank(message = "Maker ID is mandatory")
    @Size(max = 20, message = "Maker ID cannot exceed 20 characters")
    private String makerId;
}
