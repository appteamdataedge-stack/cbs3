package com.example.moneymarket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for generating financial reports during EOD
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EODReportingService {

    private static final String REPORTS_DIR = "reports";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * Generate financial reports for the given date
     */
    @Transactional(readOnly = true)
    public boolean generateFinancialReports(LocalDate systemDate) {
        log.info("Starting financial reports generation for date: {}", systemDate);
        
        try {
            // Create reports directory if it doesn't exist
            String dateStr = systemDate.format(DATE_FORMATTER);
            Path reportsPath = Paths.get(REPORTS_DIR, dateStr);
            Files.createDirectories(reportsPath);
            
            // Generate Trial Balance
            boolean trialBalanceGenerated = generateTrialBalance(systemDate, reportsPath);
            
            // Generate Balance Sheet
            boolean balanceSheetGenerated = generateBalanceSheet(systemDate, reportsPath);
            
            if (trialBalanceGenerated && balanceSheetGenerated) {
                log.info("Financial reports generated successfully for date: {}", systemDate);
                return true;
            } else {
                log.error("Failed to generate some financial reports for date: {}", systemDate);
                return false;
            }
            
        } catch (Exception e) {
            log.error("Error generating financial reports for date {}: {}", systemDate, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Generate Trial Balance report
     */
    private boolean generateTrialBalance(LocalDate systemDate, Path reportsPath) {
        try {
            String fileName = String.format("TrialBalance_%s.csv", systemDate.format(DATE_FORMATTER));
            Path filePath = reportsPath.resolve(fileName);
            
            try (FileWriter writer = new FileWriter(filePath.toFile())) {
                // Write CSV header
                writer.append("GL_Code,GL_Name,Debit_Amount,Credit_Amount\n");
                
                // TODO: Implement actual GL balance retrieval and formatting
                // This is a placeholder implementation
                writer.append("110101001,Call Money - Overnight,1000000.00,0.00\n");
                writer.append("110101002,Call Money - Weekly,500000.00,0.00\n");
                writer.append("210101001,Term Money - Monthly,0.00,1500000.00\n");
                
                writer.flush();
            }
            
            log.info("Trial Balance report generated: {}", filePath);
            return true;
            
        } catch (IOException e) {
            log.error("Error generating Trial Balance report: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Generate Balance Sheet report
     */
    private boolean generateBalanceSheet(LocalDate systemDate, Path reportsPath) {
        try {
            String fileName = String.format("BalanceSheet_%s.csv", systemDate.format(DATE_FORMATTER));
            Path filePath = reportsPath.resolve(fileName);
            
            try (FileWriter writer = new FileWriter(filePath.toFile())) {
                // Write CSV header
                writer.append("Category,GL_Code,GL_Name,Amount\n");
                
                // TODO: Implement actual balance sheet generation
                // This is a placeholder implementation
                writer.append("Assets,110101001,Call Money - Overnight,1000000.00\n");
                writer.append("Assets,110101002,Call Money - Weekly,500000.00\n");
                writer.append("Liabilities,210101001,Term Money - Monthly,1500000.00\n");
                
                writer.flush();
            }
            
            log.info("Balance Sheet report generated: {}", filePath);
            return true;
            
        } catch (IOException e) {
            log.error("Error generating Balance Sheet report: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Validate that total debits equal total credits
     */
    private boolean validateTrialBalance(List<GLBalanceData> glBalances) {
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;
        
        for (GLBalanceData balance : glBalances) {
            if (balance.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                totalDebits = totalDebits.add(balance.getAmount());
            } else {
                totalCredits = totalCredits.add(balance.getAmount().abs());
            }
        }
        
        boolean isValid = totalDebits.compareTo(totalCredits) == 0;
        log.info("Trial Balance validation - Total Debits: {}, Total Credits: {}, Valid: {}", 
                totalDebits, totalCredits, isValid);
        
        return isValid;
    }

    /**
     * Validate that Assets = Liabilities + (Income - Expenditure)
     */
    private boolean validateBalanceSheet(List<GLBalanceData> glBalances) {
        BigDecimal totalAssets = BigDecimal.ZERO;
        BigDecimal totalLiabilities = BigDecimal.ZERO;
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpenditure = BigDecimal.ZERO;
        
        for (GLBalanceData balance : glBalances) {
            String glCode = balance.getGlCode();
            BigDecimal amount = balance.getAmount();
            
            if (glCode.startsWith("1")) { // Assets
                totalAssets = totalAssets.add(amount);
            } else if (glCode.startsWith("2")) { // Liabilities
                totalLiabilities = totalLiabilities.add(amount);
            } else if (glCode.startsWith("3")) { // Income
                totalIncome = totalIncome.add(amount);
            } else if (glCode.startsWith("4")) { // Expenditure
                totalExpenditure = totalExpenditure.add(amount);
            }
        }
        
        BigDecimal liabilitiesPlusEquity = totalLiabilities.add(totalIncome.subtract(totalExpenditure));
        boolean isValid = totalAssets.compareTo(liabilitiesPlusEquity) == 0;
        
        log.info("Balance Sheet validation - Assets: {}, Liabilities: {}, Income: {}, Expenditure: {}, " +
                "Liabilities + (Income - Expenditure): {}, Valid: {}", 
                totalAssets, totalLiabilities, totalIncome, totalExpenditure, liabilitiesPlusEquity, isValid);
        
        return isValid;
    }

    /**
     * GL Balance Data class for reporting
     */
    public static class GLBalanceData {
        private final String glCode;
        private final String glName;
        private final BigDecimal amount;

        public GLBalanceData(String glCode, String glName, BigDecimal amount) {
            this.glCode = glCode;
            this.glName = glName;
            this.amount = amount;
        }

        public String getGlCode() { return glCode; }
        public String getGlName() { return glName; }
        public BigDecimal getAmount() { return amount; }
    }
}
