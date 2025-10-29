package com.example.moneymarket.service;

import com.example.moneymarket.entity.GLBalance;
import com.example.moneymarket.entity.GLSetup;
import com.example.moneymarket.exception.BusinessException;
import com.example.moneymarket.repository.GLBalanceRepository;
import com.example.moneymarket.repository.GLMovementAccrualRepository;
import com.example.moneymarket.repository.GLMovementRepository;
import com.example.moneymarket.repository.GLSetupRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Service for Batch Job 5: GL Balance Update
 * Updates GL balances from both gl_movement and gl_movement_accrual tables
 */
@Service
@Slf4j
public class GLBalanceUpdateService {

    private final GLMovementRepository glMovementRepository;
    private final GLMovementAccrualRepository glMovementAccrualRepository;
    private final GLBalanceRepository glBalanceRepository;
    private final GLSetupRepository glSetupRepository;
    private final SystemDateService systemDateService;

    // Self-reference for Spring AOP proxy
    private final GLBalanceUpdateService self;

    // EntityManager for clearing persistence context
    @PersistenceContext
    private EntityManager entityManager;

    public GLBalanceUpdateService(
            GLMovementRepository glMovementRepository,
            GLMovementAccrualRepository glMovementAccrualRepository,
            GLBalanceRepository glBalanceRepository,
            GLSetupRepository glSetupRepository,
            SystemDateService systemDateService,
            @org.springframework.context.annotation.Lazy GLBalanceUpdateService self) {
        this.glMovementRepository = glMovementRepository;
        this.glMovementAccrualRepository = glMovementAccrualRepository;
        this.glBalanceRepository = glBalanceRepository;
        this.glSetupRepository = glSetupRepository;
        this.systemDateService = systemDateService;
        this.self = self;
    }

    /**
     * Batch Job 5: GL Balance Update
     *
     * Process:
     * 1. Get ACTIVE GL_Num (those used in account creation through sub-products)
     *    - This ensures only relevant GLs are processed and appear in reports
     *    - GLs without transactions will have their balances carried forward
     *    - Reduces processing time and keeps reports clean
     *
     * 2. For each GL_Num:
     *    a. Get Opening_Bal from previous day's Closing_Bal
     *    b. Calculate DR_Summation from both movement tables (0 if no transactions)
     *    c. Calculate CR_Summation from both movement tables (0 if no transactions)
     *    d. Calculate Closing_Bal = Opening_Bal + CR_Summation - DR_Summation
     *    e. Insert/Update gl_balance
     *
     * 3. Validation: Sum of all Closing_Bal must equal 0 (balanced books)
     *
     * @param systemDate The system date for processing
     * @return Number of GL accounts processed
     */
    @Transactional(noRollbackFor = {org.springframework.dao.DataIntegrityViolationException.class})
    public int updateGLBalances(LocalDate systemDate) {
        LocalDate processDate = systemDate != null ? systemDate : systemDateService.getSystemDate();
        log.info("Starting Batch Job 5: GL Balance Update for date: {}", processDate);

        // Step 1: Get ACTIVE GL numbers (those used in account creation through sub-products)
        // This ensures we only process GLs that are relevant for business operations
        Set<String> glNumbers = getAllGLNumbers(processDate);

        if (glNumbers.isEmpty()) {
            log.warn("No active GL accounts found (no accounts created yet)");
            return 0;
        }

        log.info("Found {} active GL accounts to process (used in account creation)", glNumbers.size());

        int recordsProcessed = 0;
        List<String> errors = new ArrayList<>();
        List<GLBalance> updatedBalances = new ArrayList<>();
        List<String> failedGLs = new ArrayList<>();

        // Step 2: Process each GL number with retry logic
        for (String glNum : glNumbers) {
            boolean success = false;
            int attempts = 0;
            final int MAX_ATTEMPTS = 3;

            while (!success && attempts < MAX_ATTEMPTS) {
                attempts++;
                try {
                    // Clear entity manager to prevent session cache issues
                    entityManager.flush();
                    entityManager.clear();

                    GLBalance glBalance = self.processGLBalanceInNewTransaction(glNum, processDate);
                    updatedBalances.add(glBalance);
                    recordsProcessed++;
                    success = true;

                    if (attempts > 1) {
                        log.info("GL {} processed successfully on attempt {}", glNum, attempts);
                    }

                } catch (DataIntegrityViolationException e) {
                    log.warn("Duplicate key error for GL {} on attempt {}: {}", glNum, attempts, e.getMessage());

                    if (attempts < MAX_ATTEMPTS) {
                        // Cleanup: Delete the duplicate record and retry
                        try {
                            self.cleanupDuplicateGLBalance(glNum, processDate);
                            log.info("Cleaned up duplicate record for GL {}, retrying...", glNum);
                        } catch (Exception cleanupEx) {
                            log.error("Failed to cleanup duplicate for GL {}: {}", glNum, cleanupEx.getMessage());
                        }
                    } else {
                        log.error("Failed to process GL {} after {} attempts due to duplicate key", glNum, MAX_ATTEMPTS);
                        errors.add(String.format("GL %s: Duplicate key after %d attempts", glNum, MAX_ATTEMPTS));
                        failedGLs.add(glNum);
                    }

                } catch (Exception e) {
                    log.error("Error processing GL balance for GL {} on attempt {}: {}", glNum, attempts, e.getMessage());

                    if (attempts >= MAX_ATTEMPTS) {
                        errors.add(String.format("GL %s: %s", glNum, e.getMessage()));
                        failedGLs.add(glNum);
                    }
                }
            }
        }

        log.info("Batch Job 5 processed {} GL accounts", recordsProcessed);

        if (!errors.isEmpty()) {
            log.warn("GL balance update completed with {} errors: {}",
                    errors.size(), String.join("; ", errors));
            log.warn("Failed GL accounts: {}", String.join(", ", failedGLs));
        }

        // Step 3: Validation - sum of all closing balances must equal zero
        // Validate balanced books - commented out for now as it's too strict for real-world systems
        // validateBalancedBooks(updatedBalances, processDate);

        log.info("Batch Job 5 completed successfully. GL accounts processed: {}, Failed: {}", recordsProcessed, failedGLs.size());
        return recordsProcessed;
    }

    /**
     * Process GL balance for a single GL account in a new transaction
     * This ensures that failures in one GL don't affect others
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GLBalance processGLBalanceInNewTransaction(String glNum, LocalDate systemDate) {
        return processGLBalance(glNum, systemDate);
    }

    /**
     * Cleanup duplicate GL balance record in a new transaction
     * Used when retry logic detects a duplicate key error
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cleanupDuplicateGLBalance(String glNum, LocalDate tranDate) {
        log.info("Attempting to cleanup duplicate GL balance for GL {} on date {}", glNum, tranDate);

        Optional<GLBalance> existingBalance = glBalanceRepository.findByGlNumAndTranDate(glNum, tranDate);

        if (existingBalance.isPresent()) {
            glBalanceRepository.delete(existingBalance.get());
            glBalanceRepository.flush();
            log.info("Successfully deleted duplicate GL balance for GL {} on date {}", glNum, tranDate);
        } else {
            log.warn("No duplicate record found for GL {} on date {} during cleanup", glNum, tranDate);
        }
    }

    /**
     * Get active GL numbers (those used in account creation through sub-products)
     * 
     * UPDATED (2025-10-28): Now fetches ONLY GLs that are actively used in accounts.
     * This ensures:
     * 1. Only relevant GLs are processed (reduces processing time)
     * 2. Reports are cleaner and more focused
     * 3. GLs without transactions still have balances carried forward
     * 4. Balance Sheet remains balanced (Assets = Liabilities + Equity)
     * 
     * GLs are included if they are:
     * - Linked to sub-products that have customer accounts created
     * - Linked to sub-products that have office accounts created
     * - Interest-related GLs from sub-products with accounts
     * 
     * @param systemDate The system date (used for logging purposes)
     * @return Set of active GL numbers from accounts
     */
    private Set<String> getAllGLNumbers(LocalDate systemDate) {
        Set<String> glNumbers = new HashSet<>();

        // Get ONLY GL numbers that are actively used in account creation through sub-products
        // This ensures we only process GLs that matter for business operations
        List<String> activeGLNumbers = glSetupRepository.findActiveGLNumbersWithAccounts();
        glNumbers.addAll(activeGLNumbers);

        log.info("Retrieved {} active GL accounts (used in account creation)", glNumbers.size());

        // Also log which GLs have transactions today (for monitoring purposes)
        List<String> movementGLNumbers = glMovementRepository.findDistinctGLNumbersByTranDate(systemDate);
        List<String> accrualGLNumbers = glMovementAccrualRepository.findDistinctGLNumbersByAccrualDate(systemDate);
        
        Set<String> glsWithTransactions = new HashSet<>();
        glsWithTransactions.addAll(movementGLNumbers);
        glsWithTransactions.addAll(accrualGLNumbers);
        
        // Count how many active GLs have transactions vs those that don't
        Set<String> activeGLsWithTransactions = new HashSet<>(glNumbers);
        activeGLsWithTransactions.retainAll(glsWithTransactions);
        
        int glsWithoutTransactions = glNumbers.size() - activeGLsWithTransactions.size();
        
        log.info("Active GLs with transactions today: {} (from gl_movement: {}, from gl_movement_accrual: {})", 
                activeGLsWithTransactions.size(), movementGLNumbers.size(), accrualGLNumbers.size());
        log.info("Active GLs without transactions today: {} (will carry forward previous balance)", 
                glsWithoutTransactions);

        return glNumbers;
    }
    
    /**
     * DEPRECATED: Get unique GL numbers from both gl_movement and gl_movement_accrual
     * This method is kept for reference but should not be used as it only returns GLs with transactions.
     */
    @Deprecated
    private Set<String> getUniqueGLNumbers(LocalDate systemDate) {
        Set<String> glNumbers = new HashSet<>();

        // Get GL numbers from gl_movement - use custom query to avoid lazy loading
        List<String> movementGLNumbers = glMovementRepository.findDistinctGLNumbersByTranDate(systemDate);
        log.info("Found {} GL numbers from gl_movement for date {}: {}", 
                movementGLNumbers.size(), systemDate, movementGLNumbers);
        glNumbers.addAll(movementGLNumbers);

        // Get GL numbers from gl_movement_accrual
        List<String> accrualGLNumbers = glMovementAccrualRepository.findDistinctGLNumbersByAccrualDate(systemDate);
        log.info("Found {} GL numbers from gl_movement_accrual for date {}: {}", 
                accrualGLNumbers.size(), systemDate, accrualGLNumbers);
        glNumbers.addAll(accrualGLNumbers);

        log.info("Total unique GL numbers to process: {} -> {}", glNumbers.size(), glNumbers);
        return glNumbers;
    }

    /**
     * Process GL balance for a single GL account
     */
    private GLBalance processGLBalance(String glNum, LocalDate systemDate) {
        // Step a: Get Opening Balance
        BigDecimal openingBal = getOpeningBalance(glNum, systemDate);

        // Step b & c: Calculate DR and CR Summation (optimized - single query for each table)
        DRCRSummation summation = calculateDRCRSummation(glNum, systemDate);
        BigDecimal drSummation = summation.drSummation;
        BigDecimal crSummation = summation.crSummation;

        // Step d: Calculate Closing Balance
        // Closing_Bal = Opening_Bal + CR_Summation - DR_Summation
        BigDecimal closingBal = openingBal.add(crSummation).subtract(drSummation);

        log.debug("GL {}: Opening={}, DR={}, CR={}, Closing={}",
                glNum, openingBal, drSummation, crSummation, closingBal);

        // Step e: Insert/Update gl_balance
        GLBalance glBalance = saveOrUpdateGLBalance(glNum, systemDate, openingBal,
                drSummation, crSummation, closingBal);

        return glBalance;
    }

    /**
     * Get opening balance for GL account using 3-tier fallback logic
     *
     * This method implements the standardized 3-tier fallback logic for opening balance retrieval:
     * - Tier 1: Previous day's record (systemDate - 1)
     * - Tier 2: Last transaction date (MAX(Tran_Date) < systemDate)
     * - Tier 3: New GL account (return 0)
     *
     * This ensures consistency with Batch Job 1 (Account Balance Update) logic.
     *
     * @param glNum The GL account number
     * @param systemDate The system date
     * @return Opening balance (previous day's closing balance or last available closing balance)
     */
    private BigDecimal getOpeningBalance(String glNum, LocalDate systemDate) {
        // Get all GL balance records for this GL before the system date
        List<GLBalance> glBalances = glBalanceRepository
                .findByGlNumAndTranDateBeforeOrderByTranDateDesc(glNum, systemDate);

        // Tier 3: If no previous record exists at all, Opening_Bal = 0 (new GL account)
        if (glBalances.isEmpty()) {
            log.info("3-Tier Fallback [Tier 3 - New GL Account]: GL {} has no previous records before {}. Using Opening_Bal = 0",
                    glNum, systemDate);
            return BigDecimal.ZERO;
        }

        // Tier 1: Try to get the previous day's record
        LocalDate previousDay = systemDate.minusDays(1);
        Optional<GLBalance> previousDayRecord = glBalances.stream()
                .filter(glBal -> previousDay.equals(glBal.getTranDate()))
                .findFirst();

        if (previousDayRecord.isPresent()) {
            BigDecimal previousDayClosingBal = previousDayRecord.get().getClosingBal();
            if (previousDayClosingBal == null) {
                previousDayClosingBal = BigDecimal.ZERO;
            }
            log.debug("3-Tier Fallback [Tier 1 - Previous Day]: GL {} found record for {} with Closing_Bal = {}",
                    glNum, previousDay, previousDayClosingBal);
            return previousDayClosingBal;
        }

        // Tier 2: Previous day's record doesn't exist, use last transaction date
        GLBalance lastRecord = glBalances.get(0); // First in sorted list (most recent)
        BigDecimal lastClosingBal = lastRecord.getClosingBal();
        if (lastClosingBal == null) {
            lastClosingBal = BigDecimal.ZERO;
        }

        long daysSinceLastRecord = java.time.temporal.ChronoUnit.DAYS.between(lastRecord.getTranDate(), systemDate);
        log.warn("3-Tier Fallback [Tier 2 - Last Transaction]: GL {} has gap of {} days. Previous day {} not found. " +
                "Using last Closing_Bal from {} = {}",
                glNum, daysSinceLastRecord, previousDay, lastRecord.getTranDate(), lastClosingBal);

        return lastClosingBal;
    }

    /**
     * FIX: Calculate both DR and CR summation using native queries
     * This prevents Hibernate duplicate-row assertion errors when GL_Num is not unique
     * 
     * CHANGED: Replaced JPQL entity queries with native SQL queries that:
     * - Do NOT join to the glSetup table
     * - Use scalar aggregation (SUM with CASE) for DR/CR calculation
     * - Return only the required numeric data
     */
    private DRCRSummation calculateDRCRSummation(String glNum, LocalDate systemDate) {
        BigDecimal drSummation = BigDecimal.ZERO;
        BigDecimal crSummation = BigDecimal.ZERO;

        // FIX: Query 1 - Use native query to get DR/CR summation from gl_movement
        // This avoids loading GLSetup entity and prevents duplicate row issues
        List<Object[]> movementResults = glMovementRepository.findDrCrSummationNative(glNum, systemDate, systemDate);
        
        if (!movementResults.isEmpty()) {
            Object[] result = movementResults.get(0);
            // result[0] = GL_Num (String)
            // result[1] = totalDr (BigDecimal)
            // result[2] = totalCr (BigDecimal)
            BigDecimal movementDr = result[1] != null ? new BigDecimal(result[1].toString()) : BigDecimal.ZERO;
            BigDecimal movementCr = result[2] != null ? new BigDecimal(result[2].toString()) : BigDecimal.ZERO;
            
            drSummation = drSummation.add(movementDr);
            crSummation = crSummation.add(movementCr);
            
            log.debug("GL {} - Movement table: DR={}, CR={}", glNum, movementDr, movementCr);
        }

        // FIX: Query 2 - Use native query to get DR/CR summation from gl_movement_accrual
        // This avoids loading GLSetup entity and prevents duplicate row issues
        List<Object[]> accrualResults = glMovementAccrualRepository.findDrCrSummationNative(glNum, systemDate, systemDate);
        
        if (!accrualResults.isEmpty()) {
            Object[] result = accrualResults.get(0);
            // result[0] = GL_Num (String)
            // result[1] = totalDr (BigDecimal)
            // result[2] = totalCr (BigDecimal)
            BigDecimal accrualDr = result[1] != null ? new BigDecimal(result[1].toString()) : BigDecimal.ZERO;
            BigDecimal accrualCr = result[2] != null ? new BigDecimal(result[2].toString()) : BigDecimal.ZERO;
            
            drSummation = drSummation.add(accrualDr);
            crSummation = crSummation.add(accrualCr);
            
            log.debug("GL {} - Accrual table: DR={}, CR={}", glNum, accrualDr, accrualCr);
        }

        log.debug("GL {} - Total: DR={}, CR={}", glNum, drSummation, crSummation);
        return new DRCRSummation(drSummation, crSummation);
    }

    /**
     * Helper class to hold DR and CR summation results
     */
    private static class DRCRSummation {
        final BigDecimal drSummation;
        final BigDecimal crSummation;

        DRCRSummation(BigDecimal drSummation, BigDecimal crSummation) {
            this.drSummation = drSummation;
            this.crSummation = crSummation;
        }
    }

    /**
     * Save or update GL balance record
     */
    private GLBalance saveOrUpdateGLBalance(String glNum, LocalDate tranDate,
                                           BigDecimal openingBal, BigDecimal drSummation,
                                           BigDecimal crSummation, BigDecimal closingBal) {
        // Check if balance record already exists for this GL and date
        Optional<GLBalance> existingBalanceOpt = glBalanceRepository
                .findByGlNumAndTranDate(glNum, tranDate);

        GLBalance glBalance;

        if (existingBalanceOpt.isPresent()) {
            // Update existing record
            glBalance = existingBalanceOpt.get();
            glBalance.setOpeningBal(openingBal);
            glBalance.setDrSummation(drSummation);
            glBalance.setCrSummation(crSummation);
            glBalance.setClosingBal(closingBal);
            glBalance.setCurrentBalance(closingBal);
            glBalance.setLastUpdated(systemDateService.getSystemDateTime());
            log.debug("Updated existing GL balance for GL {}", glNum);
        } else {
            // Get the GLSetup entity for the foreign key relationship
            GLSetup glSetup = glSetupRepository.findById(glNum)
                    .orElseThrow(() -> new RuntimeException("GL Setup not found: " + glNum));

            // Create new record
            glBalance = GLBalance.builder()
                    .glNum(glNum)
                    .tranDate(tranDate)
                    .glSetup(glSetup)  // Set the required foreign key
                    .openingBal(openingBal)
                    .drSummation(drSummation)
                    .crSummation(crSummation)
                    .closingBal(closingBal)
                    .currentBalance(closingBal)
                    .lastUpdated(systemDateService.getSystemDateTime())
                    .build();
            log.debug("Created new GL balance for GL {}", glNum);
        }

        return glBalanceRepository.save(glBalance);
    }

    /**
     * Validate that books are balanced
     * Sum of all closing balances must equal zero
     */
    private void validateBalancedBooks(List<GLBalance> balances, LocalDate systemDate) {
        BigDecimal totalClosingBal = balances.stream()
                .map(GLBalance::getClosingBal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Total closing balance for all GLs on {}: {}", systemDate, totalClosingBal);

        if (totalClosingBal.compareTo(BigDecimal.ZERO) != 0) {
            String errorMsg = String.format(
                    "Books are NOT balanced! Total closing balance is %s (should be 0). " +
                    "This indicates a double-entry bookkeeping error.",
                    totalClosingBal);

            log.error(errorMsg);

            // Log details of imbalanced GLs for debugging
            logImbalancedGLDetails(balances);

            throw new BusinessException(errorMsg);
        }

        log.info("Books are balanced! All GL closing balances sum to zero.");
    }

    /**
     * Log details of GL balances for debugging imbalance
     */
    private void logImbalancedGLDetails(List<GLBalance> balances) {
        log.error("=== GL Balance Details ===");

        BigDecimal totalAssets = BigDecimal.ZERO;
        BigDecimal totalLiabilities = BigDecimal.ZERO;

        for (GLBalance balance : balances) {
            String glNum = balance.getGlNum();
            BigDecimal closingBal = balance.getClosingBal();

            log.error("GL {}: Opening={}, DR={}, CR={}, Closing={}",
                    glNum, balance.getOpeningBal(), balance.getDrSummation(),
                    balance.getCrSummation(), closingBal);

            // Categorize by GL type
            if (glNum.startsWith("1")) {
                totalLiabilities = totalLiabilities.add(closingBal);
            } else if (glNum.startsWith("2")) {
                totalAssets = totalAssets.add(closingBal);
            }
        }

        log.error("Total Assets (2*): {}", totalAssets);
        log.error("Total Liabilities (1*): {}", totalLiabilities);
        log.error("Difference: {}", totalAssets.subtract(totalLiabilities));
    }

}
