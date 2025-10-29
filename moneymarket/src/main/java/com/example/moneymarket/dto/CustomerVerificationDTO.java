package com.example.moneymarket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerVerificationDTO {

    @NotBlank(message = "Verifier ID is mandatory")
    @Size(max = 20, message = "Verifier ID cannot exceed 20 characters")
    private String verifierId;
}
