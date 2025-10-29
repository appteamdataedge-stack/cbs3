package com.example.moneymarket.validation;

import com.example.moneymarket.dto.CustomerRequestDTO;
import com.example.moneymarket.entity.CustMaster.CustomerType;
import com.example.moneymarket.repository.CustMasterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class CustomerValidator implements Validator {

    private final CustMasterRepository custMasterRepository;

    @Autowired
    public CustomerValidator(CustMasterRepository custMasterRepository) {
        this.custMasterRepository = custMasterRepository;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return CustomerRequestDTO.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        CustomerRequestDTO customer = (CustomerRequestDTO) target;

        // External Customer ID uniqueness validation removed

        // Validate customer name fields based on customer type
        if (customer.getCustType() == CustomerType.Individual) {
            if (customer.getFirstName() == null || customer.getFirstName().trim().isEmpty()) {
                errors.rejectValue("firstName", "required.firstName",
                        "First Name is required for Individual customer");
            }

            if (customer.getLastName() == null || customer.getLastName().trim().isEmpty()) {
                errors.rejectValue("lastName", "required.lastName",
                        "Last Name is required for Individual customer");
            }
        } else if (customer.getCustType() == CustomerType.Corporate || customer.getCustType() == CustomerType.Bank) {
            if (customer.getTradeName() == null || customer.getTradeName().trim().isEmpty()) {
                errors.rejectValue("tradeName", "required.tradeName",
                        "Trade Name is required for Corporate or Bank customer");
            }
        }

        // Validate mobile number format
        if (customer.getMobile() != null && !customer.getMobile().isEmpty()) {
            if (!customer.getMobile().matches("^[0-9]{1,15}$")) {
                errors.rejectValue("mobile", "invalid.mobile",
                        "Mobile number must contain only digits and cannot exceed 15 digits");
            }
        }
    }
}
