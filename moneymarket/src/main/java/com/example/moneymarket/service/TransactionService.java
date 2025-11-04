package com.example.moneymarket.service;

import com.example.moneymarket.dto.TransactionLineDTO;
import com.example.moneymarket.dto.TransactionLineResponseDTO;
import com.example.moneymarket.dto.TransactionRequestDTO;
import com.example.moneymarket.dto.TransactionResponseDTO;
import com.example.moneymarket.entity.CustAcctMaster;
import com.example.moneymarket.entity.GLMovement;
import com.example.moneymarket.entity.GLSetup;
import com.example.moneymarket.entity.TranTable;
import com.example.moneymarket.entity.TranTable.DrCrFlag;
import com.example.moneymarket.entity.TranTable.TranStatus;
import com.example.moneymarket.exception.BusinessException;
import com.example.moneymarket.exception.ResourceNotFoundException;
import com.example.moneymarket.repository.CustAcctMasterRepository;
import com.example.moneymarket.repository.GLMovementRepository;
import com.example.moneymarket.repository.GLSetupRepository;
import com.example.moneymarket.repository.TranTableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for transaction operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TranTableRepository tranTableRepository;
    private final CustAcctMasterRepository custAcctMasterRepository;
    private final GLMovementRepository glMovementRepository;
    private final GLSetupRepository glSetupRepository;
    private final BalanceService balanceService;
    private final TransactionValidationService validationService;
    private final SystemDateService systemDateService;
    private final UnifiedAccountService unifiedAccountService;
    private final TransactionHistoryService transactionHistoryService;

    private final Random random = new Random();

    /**
     * Create a new transaction with Entry status (Maker-Checker workflow)
     * 
     * @param transactionRequestDTO The transaction data
     * @return The created transaction response
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public TransactionResponseDTO createTransaction(TransactionRequestDTO transactionRequestDTO) {
        // Validate transaction balance
        validateTransactionBalance(transactionRequestDTO);
        
        // Validate all transactions using new business rules
        for (TransactionLineDTO lineDTO : transactionRequestDTO.getLines()) {
            try {
                validationService.validateTransaction(
                        lineDTO.getAccountNo(), lineDTO.getDrCrFlag(), lineDTO.getLcyAmt());
            } catch (BusinessException e) {
                throw new BusinessException("Transaction validation failed for account " + 
                        lineDTO.getAccountNo() + ": " + e.getMessage());
            }
        }
        
        // Generate a transaction ID
        String tranId = generateTransactionId();
        LocalDate tranDate = systemDateService.getSystemDate();
        LocalDate valueDate = transactionRequestDTO.getValueDate();
        
        List<TranTable> transactions = new ArrayList<>();
        
        // Process each transaction line - create in Entry status
        int lineNumber = 1;
        for (TransactionLineDTO lineDTO : transactionRequestDTO.getLines()) {
            // Validate account exists (customer or office account)
            if (!unifiedAccountService.accountExists(lineDTO.getAccountNo())) {
                throw new ResourceNotFoundException("Account", "Account Number", lineDTO.getAccountNo());
            }
            
            // Get account info for GL number
            UnifiedAccountService.AccountInfo accountInfo = unifiedAccountService.getAccountInfo(lineDTO.getAccountNo());
            
            // Create transaction record with Entry status
            String lineId = tranId + "-" + lineNumber++;
            TranTable transaction = TranTable.builder()
                    .tranId(lineId)
                    .tranDate(tranDate)
                    .valueDate(valueDate)
                    .drCrFlag(lineDTO.getDrCrFlag())
                    .tranStatus(TranStatus.Entry)  // Initial status is Entry (Maker)
                    .accountNo(lineDTO.getAccountNo())
                    .tranCcy(lineDTO.getTranCcy())
                    .fcyAmt(lineDTO.getFcyAmt())
                    .exchangeRate(lineDTO.getExchangeRate())
                    .lcyAmt(lineDTO.getLcyAmt())
                    .debitAmount(lineDTO.getDrCrFlag() == DrCrFlag.D ? lineDTO.getLcyAmt() : BigDecimal.ZERO)
                    .creditAmount(lineDTO.getDrCrFlag() == DrCrFlag.C ? lineDTO.getLcyAmt() : BigDecimal.ZERO)
                    .narration(transactionRequestDTO.getNarration())
                    .udf1(lineDTO.getUdf1())
                    .build();
            
            transactions.add(transaction);
        }
        
        // Save all transaction lines
        tranTableRepository.saveAll(transactions);
        
        // Create response
        TransactionResponseDTO response = buildTransactionResponse(tranId, tranDate, valueDate, 
                transactionRequestDTO.getNarration(), transactions);
        
        log.info("Transaction created with ID: {} in Entry status", tranId);
        return response;
    }

    /**
     * Post a transaction (move from Entry to Posted status)
     * This updates balances and creates GL movements
     * 
     * @param tranId The transaction ID
     * @return The updated transaction response
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public TransactionResponseDTO postTransaction(String tranId) {
        // Find all transaction lines with Entry status
        List<TranTable> transactions = tranTableRepository.findAll().stream()
                .filter(t -> t.getTranId().startsWith(tranId + "-") && t.getTranStatus() == TranStatus.Entry)
                .collect(Collectors.toList());
        
        if (transactions.isEmpty()) {
            throw new ResourceNotFoundException("Transaction", "ID", tranId);
        }
        
        // Validate again before posting
        BigDecimal totalDebit = transactions.stream()
                .filter(t -> t.getDrCrFlag() == DrCrFlag.D)
                .map(TranTable::getLcyAmt)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalCredit = transactions.stream()
                .filter(t -> t.getDrCrFlag() == DrCrFlag.C)
                .map(TranTable::getLcyAmt)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new BusinessException("Cannot post unbalanced transaction");
        }
        
        // Validate all transactions again before posting using new business rules
        for (TranTable transaction : transactions) {
            try {
                validationService.validateTransaction(
                        transaction.getAccountNo(), transaction.getDrCrFlag(), transaction.getLcyAmt());
            } catch (BusinessException e) {
                throw new BusinessException("Transaction validation failed for account " + 
                        transaction.getAccountNo() + ": " + e.getMessage());
            }
        }
        
        List<GLMovement> glMovements = new ArrayList<>();
        
        // Process each transaction line - update balances and create GL movements
        for (TranTable transaction : transactions) {
            // Update status to Posted
            transaction.setTranStatus(TranStatus.Posted);
            
            // Get GL number from account (customer or office)
            String glNum = unifiedAccountService.getGlNum(transaction.getAccountNo());
            GLSetup glSetup = glSetupRepository.findById(glNum)
                    .orElseThrow(() -> new ResourceNotFoundException("GL", "GL Number", glNum));
            
            // Update account balance with validation
            validationService.updateAccountBalanceForTransaction(
                    transaction.getAccountNo(), transaction.getDrCrFlag(), transaction.getLcyAmt());
            
            // Update GL balance
            BigDecimal newGLBalance = balanceService.updateGLBalance(
                    glNum, transaction.getDrCrFlag(), transaction.getLcyAmt());
            
            // Create GL movement record
            GLMovement glMovement = GLMovement.builder()
                    .transaction(transaction)
                    .glSetup(glSetup)
                    .drCrFlag(transaction.getDrCrFlag())
                    .tranDate(transaction.getTranDate())
                    .valueDate(transaction.getValueDate())
                    .amount(transaction.getLcyAmt())
                    .balanceAfter(newGLBalance)
                    .build();
            
            glMovements.add(glMovement);
        }
        
        // Save updated transaction status
        tranTableRepository.saveAll(transactions);
        
        // Save all GL movements
        glMovementRepository.saveAll(glMovements);
        
        TranTable firstLine = transactions.get(0);
        TransactionResponseDTO response = buildTransactionResponse(tranId, firstLine.getTranDate(), 
                firstLine.getValueDate(), firstLine.getNarration(), transactions);
        
        log.info("Transaction posted with ID: {}", tranId);
        return response;
    }

    /**
     * Verify a transaction (move from any status to Verified status)
     * Simple verification logic like products/customers/subproducts
     * Also creates transaction history records for Statement of Accounts
     * 
     * @param tranId The transaction ID
     * @return The updated transaction response
     */
    @Transactional
    public TransactionResponseDTO verifyTransaction(String tranId) {
        // Find all transaction lines (any status except Verified)
        List<TranTable> transactions = tranTableRepository.findAll().stream()
                .filter(t -> t.getTranId().startsWith(tranId + "-") && t.getTranStatus() != TranStatus.Verified)
                .collect(Collectors.toList());
        
        if (transactions.isEmpty()) {
            // Check if transaction exists but already verified
            List<TranTable> existingTransactions = tranTableRepository.findAll().stream()
                    .filter(t -> t.getTranId().startsWith(tranId + "-"))
                    .collect(Collectors.toList());
            
            if (!existingTransactions.isEmpty()) {
                throw new BusinessException("Transaction is already verified.");
            } else {
                throw new ResourceNotFoundException("Transaction", "ID", tranId);
            }
        }
        
        // Update status to Verified
        transactions.forEach(t -> t.setTranStatus(TranStatus.Verified));
        tranTableRepository.saveAll(transactions);
        
        // Create transaction history records for each transaction line
        // This populates TXN_HIST_ACCT table for Statement of Accounts
        String verifierUserId = "SYSTEM"; // TODO: Get from security context when authentication is implemented
        for (TranTable transaction : transactions) {
            transactionHistoryService.createTransactionHistory(transaction, verifierUserId);
        }
        
        TranTable firstLine = transactions.get(0);
        TransactionResponseDTO response = buildTransactionResponse(tranId, firstLine.getTranDate(), 
                firstLine.getValueDate(), firstLine.getNarration(), transactions);
        
        log.info("Transaction verified with ID: {}", tranId);
        return response;
    }

    /**
     * Reverse a transaction by creating opposite entries
     * 
     * @param tranId The original transaction ID to reverse
     * @param reason The reason for reversal
     * @return The reversal transaction response
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public TransactionResponseDTO reverseTransaction(String tranId, String reason) {
        // Find all transaction lines of the original transaction
        List<TranTable> originalTransactions = tranTableRepository.findAll().stream()
                .filter(t -> t.getTranId().startsWith(tranId + "-"))
                .collect(Collectors.toList());
        
        if (originalTransactions.isEmpty()) {
            throw new ResourceNotFoundException("Transaction", "ID", tranId);
        }
        
        // Generate reversal transaction ID
        String reversalTranId = generateTransactionId();
        LocalDate tranDate = systemDateService.getSystemDate();
        LocalDate valueDate = originalTransactions.get(0).getValueDate();
        
        List<TranTable> reversalTransactions = new ArrayList<>();
        List<GLMovement> glMovements = new ArrayList<>();
        
        // Create opposite entries
        int lineNumber = 1;
        for (TranTable original : originalTransactions) {
            // Create opposite entry
            DrCrFlag oppositeDrCr = original.getDrCrFlag() == DrCrFlag.D ? DrCrFlag.C : DrCrFlag.D;
            
            String lineId = reversalTranId + "-" + lineNumber++;
            TranTable reversalTran = TranTable.builder()
                    .tranId(lineId)
                    .tranDate(tranDate)
                    .valueDate(valueDate)
                    .drCrFlag(oppositeDrCr)
                    .tranStatus(TranStatus.Verified) // Reversals are auto-verified
                    .accountNo(original.getAccountNo())
                    .tranCcy(original.getTranCcy())
                    .fcyAmt(original.getFcyAmt())
                    .exchangeRate(original.getExchangeRate())
                    .lcyAmt(original.getLcyAmt())
                    .debitAmount(oppositeDrCr == DrCrFlag.D ? original.getLcyAmt() : BigDecimal.ZERO)
                    .creditAmount(oppositeDrCr == DrCrFlag.C ? original.getLcyAmt() : BigDecimal.ZERO)
                    .narration("REVERSAL: " + reason + " (Original: " + original.getTranId() + ")")
                    .pointingId(original.getPointingId()) // Link to original
                    .udf1(original.getUdf1())
                    .build();
            
            reversalTransactions.add(reversalTran);
            
            // Get GL number from account
            CustAcctMaster account = custAcctMasterRepository.findById(original.getAccountNo())
                    .orElseThrow(() -> new ResourceNotFoundException("Account", "Account Number", original.getAccountNo()));
            
            String glNum = account.getGlNum();
            GLSetup glSetup = glSetupRepository.findById(glNum)
                    .orElseThrow(() -> new ResourceNotFoundException("GL", "GL Number", glNum));
            
            // Update account balance (opposite direction)
            balanceService.updateAccountBalance(
                    original.getAccountNo(), oppositeDrCr, original.getLcyAmt());
            
            // Update GL balance (opposite direction)
            BigDecimal newGLBalance = balanceService.updateGLBalance(
                    glNum, oppositeDrCr, original.getLcyAmt());
            
            // Create GL movement record
            GLMovement glMovement = GLMovement.builder()
                    .transaction(reversalTran)
                    .glSetup(glSetup)
                    .drCrFlag(oppositeDrCr)
                    .tranDate(tranDate)
                    .valueDate(valueDate)
                    .amount(original.getLcyAmt())
                    .balanceAfter(newGLBalance)
                    .build();
            
            glMovements.add(glMovement);
        }
        
        // Save all reversal transaction lines
        tranTableRepository.saveAll(reversalTransactions);
        
        // Save all GL movements
        glMovementRepository.saveAll(glMovements);
        
        TransactionResponseDTO response = buildTransactionResponse(reversalTranId, tranDate, valueDate, 
                "REVERSAL: " + reason, reversalTransactions);
        
        log.info("Transaction reversed. Original ID: {}, Reversal ID: {}", tranId, reversalTranId);
        return response;
    }

    /**
     * Get all transactions with pagination
     * Groups transaction lines by base transaction ID
     * 
     * @param pageable The pagination information
     * @return Page of transaction responses
     */
    public Page<TransactionResponseDTO> getAllTransactions(Pageable pageable) {
        // Get all transaction lines
        List<TranTable> allTransactions = tranTableRepository.findAll();
        
        // Group by base transaction ID (remove line number suffix)
        Map<String, List<TranTable>> groupedTransactions = allTransactions.stream()
                .collect(Collectors.groupingBy(t -> extractBaseTranId(t.getTranId())));
        
        // Convert to response DTOs
        List<TransactionResponseDTO> allResponses = groupedTransactions.entrySet().stream()
                .map(entry -> {
                    String baseTranId = entry.getKey();
                    List<TranTable> lines = entry.getValue();
                    TranTable firstLine = lines.get(0);
                    return buildTransactionResponse(baseTranId, firstLine.getTranDate(), 
                            firstLine.getValueDate(), firstLine.getNarration(), lines);
                })
                .sorted((a, b) -> b.getTranDate().compareTo(a.getTranDate())) // Sort by date descending
                .collect(Collectors.toList());
        
        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allResponses.size());
        List<TransactionResponseDTO> pageContent = start < allResponses.size() 
                ? allResponses.subList(start, end) 
                : new ArrayList<>();
        
        return new PageImpl<>(pageContent, pageable, allResponses.size());
    }

    /**
     * Get a transaction by ID
     * 
     * @param tranId The transaction ID
     * @return The transaction response
     */
    public TransactionResponseDTO getTransaction(String tranId) {
        // Find all transaction lines with the given transaction ID prefix
        List<TranTable> transactions = tranTableRepository.findAll().stream()
                .filter(t -> t.getTranId().startsWith(tranId + "-"))
                .collect(Collectors.toList());
        
        if (transactions.isEmpty()) {
            throw new ResourceNotFoundException("Transaction", "ID", tranId);
        }
        
        // Get the first line to extract common transaction data
        TranTable firstLine = transactions.get(0);
        
        // Create response
        return buildTransactionResponse(tranId, firstLine.getTranDate(), firstLine.getValueDate(), 
                firstLine.getNarration(), transactions);
    }
    
    /**
     * Extract base transaction ID (remove line number suffix)
     * Example: "T20251009123456-1" → "T20251009123456"
     * 
     * @param fullTranId The full transaction ID with line number
     * @return The base transaction ID
     */
    private String extractBaseTranId(String fullTranId) {
        int lastDashIndex = fullTranId.lastIndexOf('-');
        return lastDashIndex > 0 ? fullTranId.substring(0, lastDashIndex) : fullTranId;
    }

    /**
     * Validate that the transaction is balanced (debit equals credit)
     * 
     * @param transactionRequestDTO The transaction data
     */
    private void validateTransactionBalance(TransactionRequestDTO transactionRequestDTO) {
        BigDecimal totalDebits = transactionRequestDTO.getLines().stream()
                .filter(line -> line.getDrCrFlag() == DrCrFlag.D)
                .map(TransactionLineDTO::getLcyAmt)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
        BigDecimal totalCredits = transactionRequestDTO.getLines().stream()
                .filter(line -> line.getDrCrFlag() == DrCrFlag.C)
                .map(TransactionLineDTO::getLcyAmt)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new BusinessException("Debit amount does not equal credit amount. Please correct the entries.");
        }
    }

    /**
     * Generate a unique transaction ID (max 20 characters)
     * Format: TyyyyMMddHHmmssSSS (20 chars max)
     * Example: T20251009120530123
     * 
     * @return The transaction ID
     */
    private String generateTransactionId() {
        // Replaced device-based date/time with System_Date (SystemDateService) - CBS Compliance Fix
        LocalDate systemDate = systemDateService.getSystemDate();
        String date = systemDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        // Use sequence-based approach instead of System.currentTimeMillis() for CBS compliance
        // Generate a unique sequence number based on existing transactions for the same date
        long sequenceNumber = generateSequenceNumber(systemDate);
        String sequenceComponent = String.format("%06d", sequenceNumber);
        String randomPart = String.format("%03d", random.nextInt(1000));
        
        // Format: T + yyyyMMdd + 6-digit-sequence + 3-digit-random = 18 characters
        return "T" + date + sequenceComponent + randomPart;
    }
    
    /**
     * Generate a sequence number for transaction ID based on existing transactions for the same date
     * CBS Compliance: Uses System_Date instead of device clock for deterministic ID generation
     */
    private long generateSequenceNumber(LocalDate systemDate) {
        // Count existing transactions for the same date to generate next sequence
        long existingCount = tranTableRepository.countByTranDate(systemDate);
        return existingCount + 1;
    }

    /**
     * Build a transaction response from the transaction lines
     * 
     * @param tranId The transaction ID
     * @param tranDate The transaction date
     * @param valueDate The value date
     * @param narration The narration
     * @param transactions The transaction lines
     * @return The transaction response
     */
    private TransactionResponseDTO buildTransactionResponse(String tranId, LocalDate tranDate, 
            LocalDate valueDate, String narration, List<TranTable> transactions) {
        
        List<TransactionLineResponseDTO> lines = transactions.stream()
                .map(tran -> {
                    String accountNo = tran.getAccountNo();
                    String accountName = custAcctMasterRepository.findById(accountNo)
                            .map(CustAcctMaster::getAcctName)
                            .orElse("");
                            
                    return TransactionLineResponseDTO.builder()
                            .tranId(tran.getTranId())
                            .accountNo(accountNo)
                            .accountName(accountName)
                            .drCrFlag(tran.getDrCrFlag())
                            .tranCcy(tran.getTranCcy())
                            .fcyAmt(tran.getFcyAmt())
                            .exchangeRate(tran.getExchangeRate())
                            .lcyAmt(tran.getLcyAmt())
                            .udf1(tran.getUdf1())
                            .build();
                })
                .collect(Collectors.toList());
                
        return TransactionResponseDTO.builder()
                .tranId(tranId)
                .tranDate(tranDate)
                .valueDate(valueDate)
                .narration(narration)
                .lines(lines)
                .balanced(true)
                .status(transactions.get(0).getTranStatus().toString())
                .build();
    }
}
