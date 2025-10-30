package com.example.moneymarket.service;

import com.example.moneymarket.dto.OfficeAccountRequestDTO;
import com.example.moneymarket.dto.OfficeAccountResponseDTO;
import com.example.moneymarket.entity.OFAcctMaster;
import com.example.moneymarket.entity.SubProdMaster;
import com.example.moneymarket.entity.AcctBal;
import com.example.moneymarket.exception.BusinessException;
import com.example.moneymarket.exception.ResourceNotFoundException;
import com.example.moneymarket.repository.OFAcctMasterRepository;
import com.example.moneymarket.repository.SubProdMasterRepository;
import com.example.moneymarket.repository.AcctBalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Service for office account operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OfficeAccountService {

    private final OFAcctMasterRepository ofAcctMasterRepository;
    private final SubProdMasterRepository subProdMasterRepository;
    private final AcctBalRepository acctBalRepository;
    private final AccountNumberService accountNumberService;
    private final SystemDateService systemDateService;

    /**
     * Create a new office account
     * 
     * @param accountRequestDTO The account data
     * @return The created account response
     */
    @Transactional
    public OfficeAccountResponseDTO createAccount(OfficeAccountRequestDTO accountRequestDTO) {
        // Validate sub-product exists and is active
        SubProdMaster subProduct = subProdMasterRepository.findById(accountRequestDTO.getSubProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Sub-Product", "ID", accountRequestDTO.getSubProductId()));

        if (subProduct.getSubProductStatus() != SubProdMaster.SubProductStatus.Active) {
            throw new BusinessException("Sub-Product is not active");
        }

        // Generate office account number using the new format
        String glNum = subProduct.getCumGLNum();
        String accountNo = accountNumberService.generateOfficeAccountNumber(glNum);

        // Map DTO to entity
        OFAcctMaster account = mapToEntity(accountRequestDTO, subProduct, accountNo, glNum);

        // Save the account
        OFAcctMaster savedAccount = ofAcctMasterRepository.save(account);
        
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

        log.info("Office Account created with account number: {}", savedAccount.getAccountNo());

        // Return the response
        return mapToResponse(savedAccount);
    }

    /**
     * Update an existing office account
     * 
     * @param accountNo The account number
     * @param accountRequestDTO The account data
     * @return The updated account response
     */
    @Transactional
    public OfficeAccountResponseDTO updateAccount(String accountNo, OfficeAccountRequestDTO accountRequestDTO) {
        // Find the account
        OFAcctMaster account = ofAcctMasterRepository.findById(accountNo)
                .orElseThrow(() -> new ResourceNotFoundException("Office Account", "Account Number", accountNo));

        // Validate sub-product exists
        SubProdMaster subProduct = subProdMasterRepository.findById(accountRequestDTO.getSubProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Sub-Product", "ID", accountRequestDTO.getSubProductId()));

        // Update account fields
        account.setSubProduct(subProduct);
        account.setAcctName(accountRequestDTO.getAcctName());
        account.setBranchCode(accountRequestDTO.getBranchCode());
        account.setDateClosure(accountRequestDTO.getDateClosure());
        account.setAccountStatus(accountRequestDTO.getAccountStatus());
        account.setReconciliationRequired(accountRequestDTO.getReconciliationRequired());

        // Save the updated account
        OFAcctMaster updatedAccount = ofAcctMasterRepository.save(account);

        log.info("Office Account updated with account number: {}", updatedAccount.getAccountNo());

        // Return the response
        return mapToResponse(updatedAccount);
    }

    /**
     * Get an office account by account number
     * 
     * @param accountNo The account number
     * @return The account response
     */
    @Transactional(readOnly = true)
    public OfficeAccountResponseDTO getAccount(String accountNo) {
        // Find the account
        OFAcctMaster account = ofAcctMasterRepository.findById(accountNo)
                .orElseThrow(() -> new ResourceNotFoundException("Office Account", "Account Number", accountNo));

        // Return the response
        return mapToResponse(account);
    }

    /**
     * Get all office accounts with pagination
     * 
     * @param pageable The pagination information
     * @return Page of account responses
     */
    @Transactional(readOnly = true)
    public Page<OfficeAccountResponseDTO> getAllAccounts(Pageable pageable) {
        // Get the accounts page
        Page<OFAcctMaster> accounts = ofAcctMasterRepository.findAll(pageable);

        // Map to response DTOs (within transaction to allow lazy loading)
        return accounts.map(this::mapToResponse);
    }

    /**
     * Close an office account
     * 
     * @param accountNo The account number
     * @return The closed account response
     */
    @Transactional
    public OfficeAccountResponseDTO closeAccount(String accountNo) {
        // Find the account
        OFAcctMaster account = ofAcctMasterRepository.findById(accountNo)
                .orElseThrow(() -> new ResourceNotFoundException("Office Account", "Account Number", accountNo));

        // Update account status and closure date
        account.setAccountStatus(OFAcctMaster.AccountStatus.Closed);
        account.setDateClosure(java.time.LocalDate.now());

        // Save the closed account
        OFAcctMaster closedAccount = ofAcctMasterRepository.save(account);

        log.info("Office Account closed with account number: {}", closedAccount.getAccountNo());

        // Return the response
        return mapToResponse(closedAccount);
    }

    /**
     * Map DTO to entity
     * 
     * @param dto The DTO
     * @param subProduct The sub-product
     * @param accountNo The account number
     * @param glNum The GL number
     * @return The entity
     */
    private OFAcctMaster mapToEntity(OfficeAccountRequestDTO dto, SubProdMaster subProduct, 
                                     String accountNo, String glNum) {
        return OFAcctMaster.builder()
                .accountNo(accountNo)
                .subProduct(subProduct)
                .glNum(glNum)
                .acctName(dto.getAcctName())
                .dateOpening(dto.getDateOpening())
                .dateClosure(dto.getDateClosure())
                .branchCode(dto.getBranchCode())
                .accountStatus(dto.getAccountStatus())
                .reconciliationRequired(dto.getReconciliationRequired())
                .build();
    }

    /**
     * Map entity to response DTO
     * 
     * @param entity The entity
     * @return The response DTO
     */
    private OfficeAccountResponseDTO mapToResponse(OFAcctMaster entity) {
        return OfficeAccountResponseDTO.builder()
                .accountNo(entity.getAccountNo())
                .subProductId(entity.getSubProduct() != null ? entity.getSubProduct().getSubProductId() : null)
                .subProductName(entity.getSubProduct() != null ? entity.getSubProduct().getSubProductName() : null)
                .glNum(entity.getGlNum())
                .acctName(entity.getAcctName())
                .dateOpening(entity.getDateOpening())
                .dateClosure(entity.getDateClosure())
                .branchCode(entity.getBranchCode())
                .accountStatus(entity.getAccountStatus())
                .reconciliationRequired(entity.getReconciliationRequired())
                .build();
    }
}
