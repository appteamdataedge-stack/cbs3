package com.example.moneymarket.validation;

import com.example.moneymarket.dto.TransactionRequestDTO;
import com.example.moneymarket.entity.TranTable.DrCrFlag;
import com.example.moneymarket.repository.CustAcctMasterRepository;
import com.example.moneymarket.repository.OFAcctMasterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.math.BigDecimal;

@Component
public class TransactionValidator implements Validator {

    private final CustAcctMasterRepository custAcctMasterRepository;
    private final OFAcctMasterRepository ofAcctMasterRepository;

    @Autowired
    public TransactionValidator(CustAcctMasterRepository custAcctMasterRepository, 
                               OFAcctMasterRepository ofAcctMasterRepository) {
        this.custAcctMasterRepository = custAcctMasterRepository;
        this.ofAcctMasterRepository = ofAcctMasterRepository;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return TransactionRequestDTO.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        TransactionRequestDTO transaction = (TransactionRequestDTO) target;

        if (transaction.getLines() == null || transaction.getLines().isEmpty()) {
            errors.rejectValue("lines", "empty.lines", "Transaction must have at least one line");
            return;
        }

        // Validate that all account numbers exist (check both customer and office accounts)
        transaction.getLines().forEach(line -> {
            boolean accountExists = custAcctMasterRepository.existsById(line.getAccountNo()) ||
                                  ofAcctMasterRepository.existsById(line.getAccountNo());
            
            if (!accountExists) {
                errors.rejectValue("lines", "invalid.accountNo", 
                        "Account number " + line.getAccountNo() + " does not exist");
            }
        });

        // Validate that debit equals credit
        BigDecimal totalDebits = transaction.getLines().stream()
                .filter(line -> line.getDrCrFlag() == DrCrFlag.D)
                .map(line -> line.getLcyAmt())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredits = transaction.getLines().stream()
                .filter(line -> line.getDrCrFlag() == DrCrFlag.C)
                .map(line -> line.getLcyAmt())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDebits.compareTo(totalCredits) != 0) {
            errors.reject("imbalance", 
                    "Debit amount does not equal credit amount. Please correct the entries.");
        }
    }
}
