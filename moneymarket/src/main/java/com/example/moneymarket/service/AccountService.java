package com.example.moneymarket.service;

import com.example.moneymarket.entity.CustAcctMaster;
import com.example.moneymarket.entity.OFAcctMaster;
import com.example.moneymarket.repository.CustAcctMasterRepository;
import com.example.moneymarket.repository.OFAcctMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for handling account operations across both Customer and Office accounts
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final CustAcctMasterRepository custAcctMasterRepository;
    private final OFAcctMasterRepository ofAcctMasterRepository;

    /**
     * Get all active accounts from both Cust_Acct_Master and OF_Acct_Master
     * This implements the UNION logic required by the EOD batch job specification
     * 
     * @return List of active account numbers
     */
    public List<String> getAllActiveAccountNumbers() {
        log.debug("Retrieving all active accounts from both Customer and Office account masters");
        
        // Get active customer accounts
        List<String> activeCustomerAccounts = custAcctMasterRepository
                .findByAccountStatus(CustAcctMaster.AccountStatus.Active)
                .stream()
                .map(CustAcctMaster::getAccountNo)
                .collect(Collectors.toList());
        
        // Get active office accounts
        List<String> activeOfficeAccounts = ofAcctMasterRepository
                .findByAccountStatus(OFAcctMaster.AccountStatus.Active)
                .stream()
                .map(OFAcctMaster::getAccountNo)
                .collect(Collectors.toList());
        
        // Combine both lists (UNION)
        activeCustomerAccounts.addAll(activeOfficeAccounts);
        
        log.info("Found {} active customer accounts and {} active office accounts. Total: {}", 
                activeCustomerAccounts.size() - activeOfficeAccounts.size(), 
                activeOfficeAccounts.size(), 
                activeCustomerAccounts.size());
        
        return activeCustomerAccounts;
    }

    /**
     * Check if an account exists in either Customer or Office account master
     * 
     * @param accountNo The account number to check
     * @return true if account exists and is active, false otherwise
     */
    public boolean isAccountActive(String accountNo) {
        // Check in customer accounts
        boolean isActiveCustomerAccount = custAcctMasterRepository.findById(accountNo)
                .map(account -> account.getAccountStatus() == CustAcctMaster.AccountStatus.Active)
                .orElse(false);
        
        // Check in office accounts
        boolean isActiveOfficeAccount = ofAcctMasterRepository.findById(accountNo)
                .map(account -> account.getAccountStatus() == OFAcctMaster.AccountStatus.Active)
                .orElse(false);
        
        return isActiveCustomerAccount || isActiveOfficeAccount;
    }

    /**
     * Get account details from either Customer or Office account master
     * 
     * @param accountNo The account number
     * @return Account details or null if not found
     */
    public Object getAccountDetails(String accountNo) {
        // Try customer accounts first
        return custAcctMasterRepository.findById(accountNo)
                .map(account -> (Object) account)
                .orElseGet(() -> ofAcctMasterRepository.findById(accountNo)
                        .map(account -> (Object) account)
                        .orElse(null));
    }
}
