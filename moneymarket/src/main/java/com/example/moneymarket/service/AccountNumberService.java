package com.example.moneymarket.service;

import com.example.moneymarket.entity.AccountSeq;
import com.example.moneymarket.entity.CustMaster;
import com.example.moneymarket.entity.SubProdMaster;
import com.example.moneymarket.exception.BusinessException;
import com.example.moneymarket.repository.AccountSeqRepository;
import com.example.moneymarket.repository.CustAcctMasterRepository;
import com.example.moneymarket.repository.GLSetupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service to generate account numbers according to business rules:
 * 
 * Customer Account Number (12 digits):
 * - First 8 digits = Primary Cust_Id
 * - 9th digit = Product category digit (based on Product GL_Num):
 *   1 = GL 110101000
 *   2 = GL 110102000
 *   3 = GL 110201000
 *   4 = GL 130101000
 *   5 = GL 140101000
 *   6 = GL 210201000
 *   7 = GL 240101000
 * - Last 3 digits = running sequence 001–999 per product per customer
 * 
 * Office Accounts:
 * - 1st digit = 9
 * - Next 9 digits = Sub-Product GL Code
 * - Last 2 digits = serial 00–99
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountNumberService {

    private final AccountSeqRepository accountSeqRepository;
    private final GLSetupRepository glSetupRepository;
    private final CustAcctMasterRepository custAcctMasterRepository;

    /**
     * Generates a new customer account number based on customer ID and product type
     * 
     * @param customer The customer entity
     * @param subProduct The sub-product entity
     * @return The generated account number
     */
    @Transactional
    public String generateCustomerAccountNumber(CustMaster customer, SubProdMaster subProduct) {
        if (customer == null || customer.getCustId() == null) {
            throw new BusinessException("Cannot generate account number: Customer ID is null");
        }
        
        // Get customer ID and ensure it's 8 digits
        String custId = String.format("%08d", customer.getCustId());
        if (custId.length() != 8) {
            throw new BusinessException("Customer ID must be exactly 8 digits for account number generation");
        }
        
        // Determine product type code from Product GL_Num (not SubProduct GL_Num)
        if (subProduct.getProduct() == null) {
            throw new BusinessException("Cannot generate account number: Product information is missing");
        }
        
        String productGLNum = subProduct.getProduct().getCumGLNum();
        if (productGLNum == null || productGLNum.isEmpty()) {
            throw new BusinessException("Cannot generate account number: Product GL Number is null or empty");
        }
        
        char productTypeCode = determineProductTypeCode(productGLNum);
        
        // Find the next sequence number for this customer and product type
        String prefix = custId + productTypeCode;
        Integer maxSeq = custAcctMasterRepository.findMaxSequenceForCustomerAndProductType(
                customer.getCustId(), String.valueOf(productTypeCode));
        
        int nextSeq = (maxSeq == null) ? 1 : maxSeq + 1;
        
        // Check for sequence overflow (3 digits can only go to 999)
        if (nextSeq > 999) {
            throw new BusinessException("Account number sequence for customer " + custId + 
                    " and product type " + productTypeCode + " has reached its maximum (999)");
        }
        
        // Format the sequence as 3 digits with leading zeros
        String formattedSequence = String.format("%03d", nextSeq);
        
        // Construct the account number (12 digits: 8 for custId + 1 for product type + 3 for sequence)
        String accountNumber = prefix + formattedSequence;
        
        log.info("Generated customer account number: {} for customer: {} and Product GL_Num: {} (9th digit: {})", 
                accountNumber, custId, productGLNum, productTypeCode);
        return accountNumber;
    }
    
    /**
     * Generates a new office account number based on GL number
     * 
     * @param glNum The GL number
     * @return The generated office account number
     */
    @Transactional
    public String generateOfficeAccountNumber(String glNum) {
        // Validate GL number exists
        if (!glSetupRepository.existsById(glNum)) {
            throw new BusinessException("Cannot generate office account number: GL Number " + glNum + " does not exist");
        }
        
        // Office account format: 9 + 9 digits of GL code + 2 digit sequence
        
        // Get or initialize the sequence counter with pessimistic lock to prevent race conditions
        AccountSeq accountSeq = accountSeqRepository.findByGlNumWithLock(glNum)
                .orElseGet(() -> {
                    // If no sequence exists, initialize a new one
                    AccountSeq newSeq = new AccountSeq();
                    newSeq.setGlNum(glNum);
                    newSeq.setSeqNumber(0); // Start with 0, will increment to 1
                    newSeq.setLastUpdated(LocalDateTime.now());
                    return newSeq;
                });

        // Increment the sequence counter
        int nextSequence = accountSeq.getSeqNumber() + 1;
        
        // Check for sequence overflow (2 digits can only go to 99)
        if (nextSequence > 99) {
            throw new BusinessException("Office account number sequence for GL " + glNum + " has reached its maximum (99)");
        }
        
        // Update the sequence
        accountSeq.setSeqNumber(nextSequence);
        accountSeq.setLastUpdated(LocalDateTime.now());
        accountSeqRepository.save(accountSeq);
        
        // Format the sequence as 2 digits with leading zeros
        String formattedSequence = String.format("%02d", nextSequence);
        
        // Construct the office account number (12 digits: 1 for '9' + 9 for GL + 2 for sequence)
        String accountNumber = "9" + glNum + formattedSequence;
        
        log.info("Generated office account number: {} for GL: {}", accountNumber, glNum);
        return accountNumber;
    }
    
    /**
     * Legacy method for backward compatibility
     * 
     * @param glNum The GL number to use as a prefix
     * @return The generated account number (GL_Num + sequential counter)
     */
    @Transactional
    public String generateAccountNumber(String glNum) {
        // Validate GL number exists
        if (!glSetupRepository.existsById(glNum)) {
            throw new BusinessException("Cannot generate account number: GL Number " + glNum + " does not exist");
        }

        // Get or initialize the sequence counter with pessimistic lock to prevent race conditions
        AccountSeq accountSeq = accountSeqRepository.findByGlNumWithLock(glNum)
                .orElseGet(() -> {
                    // If no sequence exists, initialize a new one
                    AccountSeq newSeq = new AccountSeq();
                    newSeq.setGlNum(glNum);
                    newSeq.setSeqNumber(0); // Start with 0, will increment to 1
                    newSeq.setLastUpdated(LocalDateTime.now());
                    return newSeq;
                });

        // Increment the sequence counter
        int nextSequence = accountSeq.getSeqNumber() + 1;
        
        // Check for sequence overflow (3 digits can only go to 999)
        if (nextSequence > 999) {
            throw new BusinessException("Account number sequence for GL " + glNum + " has reached its maximum (999)");
        }
        
        // Update the sequence
        accountSeq.setSeqNumber(nextSequence);
        accountSeq.setLastUpdated(LocalDateTime.now());
        accountSeqRepository.save(accountSeq);
        
        // Format the sequence as 3 digits with leading zeros
        String formattedSequence = String.format("%03d", nextSequence);
        
        // Construct the account number
        String accountNumber = glNum + formattedSequence;
        
        log.info("Generated account number: {} for GL: {}", accountNumber, glNum);
        return accountNumber;
    }
    
    /**
     * Determines the product type code based on the Product GL_Num
     * 
     * @param glNum The Product GL_Num
     * @return The product type code (9th digit)
     */
    private char determineProductTypeCode(String glNum) {
        if (glNum == null || glNum.isEmpty()) {
            throw new BusinessException("Invalid GL number format for product type determination");
        }
        
        // Map Product GL_Num to 9th digit based on business rules
        switch (glNum) {
            case "110101000": return '1';
            case "110102000": return '2';
            case "110201000": return '3';
            case "130101000": return '4';
            case "140101000": return '5';
            case "210201000": return '6';
            case "240101000": return '7';
            default:
                throw new BusinessException("Cannot determine product type code for Product GL_Num: " + glNum + 
                    ". Supported GL_Nums: 110101000, 110102000, 110201000, 130101000, 140101000, 210201000, 240101000");
        }
    }
}
