package com.example.moneymarket.service;

import com.example.moneymarket.entity.CustMaster.CustomerType;
import com.example.moneymarket.exception.BusinessException;
import com.example.moneymarket.repository.CustMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service to generate customer IDs according to business rules:
 * - Numeric, max length 8
 * - First digit based on customer type:
 *   1 = Individual
 *   2 = Corporate
 *   3 = Bank
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerIdService {

    private final CustMasterRepository custMasterRepository;
    
    /**
     * Generate a new customer ID based on customer type
     * 
     * @param customerType The type of customer
     * @return The generated customer ID
     */
    @Transactional
    public Integer generateCustomerId(CustomerType customerType) {
        // Determine prefix based on customer type
        int prefix;
        switch (customerType) {
            case Individual:
                prefix = 1;
                break;
            case Corporate:
                prefix = 2;
                break;
            case Bank:
                prefix = 3;
                break;
            default:
                throw new BusinessException("Invalid customer type for ID generation");
        }
        
        // Find the maximum customer ID with this prefix
        String prefixStr = String.valueOf(prefix);
        Integer maxCustId = custMasterRepository.findMaxCustIdWithPrefix(prefixStr + "%");
        
        // If no existing customer with this prefix, start with prefix followed by zeros
        int nextId;
        if (maxCustId == null) {
            nextId = prefix * 10000000; // Start with prefix followed by 7 zeros
        } else {
            nextId = maxCustId + 1;
            
            // Ensure the new ID still starts with the correct prefix
            String nextIdStr = String.valueOf(nextId);
            if (!nextIdStr.startsWith(prefixStr)) {
                // Instead of throwing an error, find the next available ID
                log.warn("Customer ID sequence reached max value for prefix {}. Finding next available ID.", prefix);
                nextId = findNextAvailableId(prefix);
            }
        }
        
        // Ensure ID doesn't exceed 8 digits
        if (String.valueOf(nextId).length() > 8) {
            throw new BusinessException("Customer ID exceeds maximum length of 8 digits");
        }
        
        log.info("Generated customer ID: {} for customer type: {}", nextId, customerType);
        return nextId;
    }
    
    /**
     * Find the next available customer ID when sequence has been exhausted
     * This method searches for gaps in the sequence or reuses deleted IDs
     * 
     * @param prefix The customer type prefix (1, 2, or 3)
     * @return The next available customer ID
     */
    private Integer findNextAvailableId(int prefix) {
        // Calculate the valid range for this prefix
        int minId = prefix * 10000000; // e.g., 10000000 for prefix 1
        int maxId = (prefix + 1) * 10000000 - 1; // e.g., 19999999 for prefix 1
        
        // Start from the beginning of the range and find the first available ID
        for (int candidateId = minId; candidateId <= maxId; candidateId++) {
            if (!custMasterRepository.existsById(candidateId)) {
                return candidateId;
            }
        }
        
        // If we reach here, there are truly no available IDs in the valid range
        throw new BusinessException("No available customer IDs for type with prefix " + prefix + 
                ". Please archive unused customers to free up IDs.");
    }
}
