package com.example.moneymarket.validation;

import com.example.moneymarket.dto.CustomerAccountRequestDTO;
import com.example.moneymarket.entity.CustAcctMaster.AccountStatus;
import com.example.moneymarket.repository.CustMasterRepository;
import com.example.moneymarket.repository.SubProdMasterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;


@Component
public class CustomerAccountValidator implements Validator {

    private final CustMasterRepository custMasterRepository;
    private final SubProdMasterRepository subProdMasterRepository;

    @Autowired
    public CustomerAccountValidator(CustMasterRepository custMasterRepository,
                                   SubProdMasterRepository subProdMasterRepository) {
        this.custMasterRepository = custMasterRepository;
        this.subProdMasterRepository = subProdMasterRepository;
    }

    @Override
    public boolean supports(@NonNull Class<?> clazz) {
        return CustomerAccountRequestDTO.class.equals(clazz);
    }

    @Override
    public void validate(@NonNull Object target, @NonNull Errors errors) {
        CustomerAccountRequestDTO account = (CustomerAccountRequestDTO) target;

        // Check if Customer ID exists
        if (!custMasterRepository.existsById(account.getCustId())) {
            errors.rejectValue("custId", "invalid.custId", 
                    "Customer ID does not exist");
        }

        // Check if Sub-Product ID exists
        if (!subProdMasterRepository.existsById(account.getSubProductId())) {
            errors.rejectValue("subProductId", "invalid.subProductId", 
                    "Sub-Product ID does not exist");
        }

        // For account closure, check if balance is zero
        if (account.getAccountStatus() == AccountStatus.Closed) {
            // This validation would be done at the service level when updating an existing account
            // Since the DTO doesn't have accountNo field, we'll check this only for update operations
            // The accountNo would be provided through the path variable in the controller
        }

        // For maturity date validation
        if (account.getTenor() != null && account.getTenor() > 0) {
            if (account.getDateMaturity() == null) {
                errors.rejectValue("dateMaturity", "required.maturity", 
                        "Maturity date is required when tenor is specified");
            }
        }
    }
}
