package com.example.moneymarket.validation;

import com.example.moneymarket.dto.CustomerVerificationDTO;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class VerificationValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return CustomerVerificationDTO.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        CustomerVerificationDTO verification = (CustomerVerificationDTO) target;
        
        // Here you would check if the maker is different from the verifier
        // This requires additional context typically provided as an additional parameter
        // For now, we'll just validate that verifierId is not blank
        
        if (verification.getVerifierId() == null || verification.getVerifierId().trim().isEmpty()) {
            errors.rejectValue("verifierId", "required.verifierId", 
                    "Verifier ID is mandatory");
        }
    }
    
    // Use this method when you have the maker ID to compare against
    public void validateMakerVerifierDifference(CustomerVerificationDTO verification, String makerId, Errors errors) {
        if (verification.getVerifierId() != null && 
            verification.getVerifierId().equals(makerId)) {
            errors.rejectValue("verifierId", "invalid.verifier", 
                    "Maker cannot verify their own record");
        }
    }
}
