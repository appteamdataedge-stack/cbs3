package com.example.moneymarket.service;

import com.example.moneymarket.dto.CustomerAccountRequestDTO;
import com.example.moneymarket.dto.CustomerAccountResponseDTO;
import com.example.moneymarket.entity.AcctBal;
import com.example.moneymarket.entity.CustAcctMaster;
import com.example.moneymarket.entity.CustMaster;
import com.example.moneymarket.entity.SubProdMaster;
import com.example.moneymarket.exception.BusinessException;
import com.example.moneymarket.exception.ResourceNotFoundException;
import com.example.moneymarket.repository.AcctBalRepository;
import com.example.moneymarket.repository.CustAcctMasterRepository;
import com.example.moneymarket.repository.CustMasterRepository;
import com.example.moneymarket.repository.SubProdMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Service for customer account operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerAccountService {

    private final CustAcctMasterRepository custAcctMasterRepository;
    private final CustMasterRepository custMasterRepository;
    private final SubProdMasterRepository subProdMasterRepository;
    private final AcctBalRepository acctBalRepository;
    private final AccountNumberService accountNumberService;
    private final SystemDateService systemDateService;
    private final BalanceService balanceService;

    /**
     * Create a new customer account
     * 
     * @param accountRequestDTO The account data
     * @return The created account response
     */
    @Transactional
    public CustomerAccountResponseDTO createAccount(CustomerAccountRequestDTO accountRequestDTO) {
        // Validate customer exists
        CustMaster customer = custMasterRepository.findById(accountRequestDTO.getCustId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "ID", accountRequestDTO.getCustId()));

        // Validate sub-product exists and is active (with Product relationship loaded)
        SubProdMaster subProduct = subProdMasterRepository.findByIdWithProduct(accountRequestDTO.getSubProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Sub-Product", "ID", accountRequestDTO.getSubProductId()));

        if (subProduct.getSubProductStatus() != SubProdMaster.SubProductStatus.Active) {
            throw new BusinessException("Sub-Product is not active");
        }

        // Apply Tenor and Date of Maturity logic
        applyTenorAndMaturityLogic(accountRequestDTO, subProduct);

        // Generate customer account number using the new format
        String accountNo = accountNumberService.generateCustomerAccountNumber(customer, subProduct);
        String glNum = subProduct.getCumGLNum();

        // Map DTO to entity
        CustAcctMaster account = mapToEntity(accountRequestDTO, customer, subProduct, accountNo, glNum);

        // Save the account
        CustAcctMaster savedAccount = custAcctMasterRepository.save(account);
        
        // Initialize account balance (Acct_Bal & opening row for daily master if required)
        AcctBal accountBalance = AcctBal.builder()
                // Replaced device-based date/time with System_Date (SystemDateService) - CBS Compliance Fix
                .tranDate(systemDateService.getSystemDate()) // Required field for composite primary key
                .accountNo(savedAccount.getAccountNo()) // Required field for composite primary key
                .currentBalance(BigDecimal.ZERO)
                .availableBalance(BigDecimal.ZERO)
                .lastUpdated(systemDateService.getSystemDateTime())
                .build();
        
        acctBalRepository.save(accountBalance);

        log.info("Customer Account created with account number: {}", savedAccount.getAccountNo());

        // Return the response with success message
        CustomerAccountResponseDTO response = mapToResponse(savedAccount, accountBalance);
        response.setMessage("Account Number " + savedAccount.getAccountNo() + " created");
        
        return response;
    }

    /**
     * Update an existing customer account
     * 
     * @param accountNo The account number
     * @param accountRequestDTO The account data
     * @return The updated account response
     */
    @Transactional
    public CustomerAccountResponseDTO updateAccount(String accountNo, CustomerAccountRequestDTO accountRequestDTO) {
        // Find the account
        CustAcctMaster account = custAcctMasterRepository.findById(accountNo)
                .orElseThrow(() -> new ResourceNotFoundException("Customer Account", "Account Number", accountNo));

        // Validate customer exists
        CustMaster customer = custMasterRepository.findById(accountRequestDTO.getCustId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "ID", accountRequestDTO.getCustId()));

        // Validate sub-product exists (with Product relationship loaded)
        SubProdMaster subProduct = subProdMasterRepository.findByIdWithProduct(accountRequestDTO.getSubProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Sub-Product", "ID", accountRequestDTO.getSubProductId()));

        // Apply Tenor and Date of Maturity logic
        applyTenorAndMaturityLogic(accountRequestDTO, subProduct);

        // Check if changing to closed status and validate balance is zero
        if (accountRequestDTO.getAccountStatus() == CustAcctMaster.AccountStatus.Closed) {
            AcctBal accountBalance = acctBalRepository.findLatestByAccountNo(accountNo)
                    .orElseThrow(() -> new ResourceNotFoundException("Account Balance", "Account Number", accountNo));
            
            if (accountBalance.getCurrentBalance().compareTo(BigDecimal.ZERO) != 0) {
                throw new BusinessException("Cannot close account with non-zero balance");
            }
        }

        // Update account fields
        account.setSubProduct(subProduct);
        account.setCustomer(customer);
        account.setCustName(accountRequestDTO.getCustName());
        account.setAcctName(accountRequestDTO.getAcctName());
        account.setTenor(accountRequestDTO.getTenor());
        account.setDateMaturity(accountRequestDTO.getDateMaturity());
        account.setDateClosure(accountRequestDTO.getDateClosure());
        account.setBranchCode(accountRequestDTO.getBranchCode());
        account.setAccountStatus(accountRequestDTO.getAccountStatus());

        // Save the updated account
        CustAcctMaster updatedAccount = custAcctMasterRepository.save(account);
        
        // Get the account balance
        AcctBal accountBalance = acctBalRepository.findLatestByAccountNo(accountNo)
                .orElseThrow(() -> new ResourceNotFoundException("Account Balance", "Account Number", accountNo));

        log.info("Customer Account updated with account number: {}", updatedAccount.getAccountNo());

        // Return the response
        return mapToResponse(updatedAccount, accountBalance);
    }

    /**
     * Get a customer account by account number
     * 
     * @param accountNo The account number
     * @return The account response
     */
    @Transactional(readOnly = true)
    public CustomerAccountResponseDTO getAccount(String accountNo) {
        // Find the account
        CustAcctMaster account = custAcctMasterRepository.findById(accountNo)
                .orElseThrow(() -> new ResourceNotFoundException("Customer Account", "Account Number", accountNo));

        // Get the account balance
        AcctBal accountBalance = acctBalRepository.findLatestByAccountNo(accountNo)
                .orElseThrow(() -> new ResourceNotFoundException("Account Balance", "Account Number", accountNo));

        // Return the response
        return mapToResponse(account, accountBalance);
    }

    /**
     * Get all customer accounts with pagination
     * 
     * @param pageable The pagination information
     * @return Page of account responses
     */
    @Transactional(readOnly = true)
    public Page<CustomerAccountResponseDTO> getAllAccounts(Pageable pageable) {
        // Get the accounts page
        Page<CustAcctMaster> accounts = custAcctMasterRepository.findAll(pageable);

        // Map to response DTOs (within transaction to allow lazy loading)
        return accounts.map(account -> {
            AcctBal balance = acctBalRepository.findLatestByAccountNo(account.getAccountNo()).orElse(
                    AcctBal.builder()
                            .accountNo(account.getAccountNo())
                            .currentBalance(BigDecimal.ZERO)
                            .availableBalance(BigDecimal.ZERO)
                            .build()
            );
            return mapToResponse(account, balance);
        });
    }

    /**
     * Close a customer account
     * 
     * @param accountNo The account number
     * @return The closed account response
     */
    @Transactional
    public CustomerAccountResponseDTO closeAccount(String accountNo) {
        // Find the account
        CustAcctMaster account = custAcctMasterRepository.findById(accountNo)
                .orElseThrow(() -> new ResourceNotFoundException("Customer Account", "Account Number", accountNo));

        // Check if balance is zero
        AcctBal accountBalance = acctBalRepository.findLatestByAccountNo(accountNo)
                .orElseThrow(() -> new ResourceNotFoundException("Account Balance", "Account Number", accountNo));
        
        if (accountBalance.getCurrentBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessException("Cannot close account with non-zero balance");
        }

        // Update account status and closure date
        account.setAccountStatus(CustAcctMaster.AccountStatus.Closed);
        // Replaced device-based date/time with System_Date (SystemDateService) - CBS Compliance Fix
        account.setDateClosure(systemDateService.getSystemDate());

        // Save the closed account
        CustAcctMaster closedAccount = custAcctMasterRepository.save(account);

        log.info("Customer Account closed with account number: {}", closedAccount.getAccountNo());

        // Return the response
        return mapToResponse(closedAccount, accountBalance);
    }

    /**
     * Map DTO to entity
     * 
     * @param dto The DTO
     * @param customer The customer
     * @param subProduct The sub-product
     * @param accountNo The account number
     * @param glNum The GL number
     * @return The entity
     */
    private CustAcctMaster mapToEntity(CustomerAccountRequestDTO dto, CustMaster customer, 
                                      SubProdMaster subProduct, String accountNo, String glNum) {
        // Set loan limit only for Asset accounts (GL starting with "2")
        BigDecimal loanLimit = BigDecimal.ZERO;
        if (glNum != null && glNum.startsWith("2") && dto.getLoanLimit() != null) {
            loanLimit = dto.getLoanLimit();
            log.info("Setting loan limit {} for Asset account {} (GL: {})", loanLimit, accountNo, glNum);
        } else if (dto.getLoanLimit() != null && dto.getLoanLimit().compareTo(BigDecimal.ZERO) > 0) {
            log.warn("Loan limit {} provided for non-Asset account {} (GL: {}). Ignoring and setting to 0.", 
                    dto.getLoanLimit(), accountNo, glNum);
        }
        
        return CustAcctMaster.builder()
                .accountNo(accountNo)
                .subProduct(subProduct)
                .glNum(glNum)
                .customer(customer)
                .custName(dto.getCustName())
                .acctName(dto.getAcctName())
                .dateOpening(dto.getDateOpening())
                .tenor(dto.getTenor())
                .dateMaturity(dto.getDateMaturity())
                .dateClosure(dto.getDateClosure())
                .branchCode(dto.getBranchCode())
                .accountStatus(dto.getAccountStatus())
                .loanLimit(loanLimit)
                .build();
    }

    /**
     * Map entity to response DTO
     * 
     * @param entity The entity
     * @param balance The account balance
     * @return The response DTO
     */
    private CustomerAccountResponseDTO mapToResponse(CustAcctMaster entity, AcctBal balance) {
        // Get computed balance (real-time balance with today's transactions)
        com.example.moneymarket.dto.AccountBalanceDTO balanceDTO = 
                balanceService.getComputedAccountBalance(entity.getAccountNo());

        return CustomerAccountResponseDTO.builder()
                .accountNo(entity.getAccountNo())
                .subProductId(entity.getSubProduct().getSubProductId())
                .subProductName(entity.getSubProduct().getSubProductName())
                .glNum(entity.getGlNum())
                .custId(entity.getCustomer().getCustId())
                .custName(entity.getCustName())
                .acctName(entity.getAcctName())
                .dateOpening(entity.getDateOpening())
                .tenor(entity.getTenor())
                .dateMaturity(entity.getDateMaturity())
                .dateClosure(entity.getDateClosure())
                .branchCode(entity.getBranchCode())
                .accountStatus(entity.getAccountStatus())
                .currentBalance(balance.getCurrentBalance())
                .availableBalance(balanceDTO.getAvailableBalance())  // Includes loan limit for Asset accounts
                .computedBalance(balanceDTO.getComputedBalance())  // Real-time balance
                .interestAccrued(balanceDTO.getInterestAccrued())  // From acct_bal_accrual
                .loanLimit(entity.getLoanLimit())  // Loan/Limit Amount for Asset accounts
                .build();
    }

    /**
     * Apply Tenor and Date of Maturity logic based on account type
     * As per BRD: Tenor and Date of Maturity fields are disabled for all running accounts (SB and CA).
     * As per BRD: Date of Maturity = Date of Opening + Tenor.
     * Alternatively, Tenor = Date of Maturity - Date of Opening.
     * 
     * @param dto The account request DTO
     * @param subProduct The sub-product entity
     */
    private void applyTenorAndMaturityLogic(CustomerAccountRequestDTO dto, SubProdMaster subProduct) {
        String glNum = subProduct.getCumGLNum();
        
        // Check if this is a running account (SB or CA)
        boolean isRunningAccount = glNum.startsWith("110101") || glNum.startsWith("110102");
        
        // As per BRD: Tenor and Date of Maturity fields are disabled for all running accounts (SB and CA).
        if (isRunningAccount) {
            dto.setTenor(null);
            dto.setDateMaturity(null);
            log.info("Running account detected (GL: {}), nullifying Tenor and Date of Maturity", glNum);
            return;
        }
        
        // Check if this is a deal-based account (TD, RD, OD/CC, TL)
        boolean isDealBasedAccount = glNum.startsWith("110201") || // Term Deposit
                                     glNum.startsWith("110202") || // Recurring Deposit
                                     glNum.startsWith("210201") || // Overdraft/CC
                                     glNum.startsWith("210202");   // Term Loan
        
        if (isDealBasedAccount) {
            // Auto-calculate based on provided input
            if (dto.getTenor() != null && dto.getDateMaturity() == null) {
                // Calculate Date of Maturity from Tenor
                LocalDate maturityDate = dto.getDateOpening().plusDays(dto.getTenor());
                dto.setDateMaturity(maturityDate);
                log.info("Auto-calculated Date of Maturity: {} (Opening: {}, Tenor: {})", 
                        maturityDate, dto.getDateOpening(), dto.getTenor());
            } else if (dto.getDateMaturity() != null && dto.getTenor() == null) {
                // Calculate Tenor from Date of Maturity
                long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(
                        dto.getDateOpening(), dto.getDateMaturity());
                dto.setTenor((int) daysBetween);
                log.info("Auto-calculated Tenor: {} days (Opening: {}, Maturity: {})", 
                        daysBetween, dto.getDateOpening(), dto.getDateMaturity());
            } else if (dto.getTenor() != null && dto.getDateMaturity() != null) {
                // Validate both fields match
                LocalDate calculatedMaturity = dto.getDateOpening().plusDays(dto.getTenor());
                if (!calculatedMaturity.equals(dto.getDateMaturity())) {
                    throw new BusinessException("Tenor and Date of Maturity do not match. Please correct the input.");
                }
            }
        }
    }
}
