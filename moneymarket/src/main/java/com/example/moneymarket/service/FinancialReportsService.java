package com.example.moneymarket.service;

import com.example.moneymarket.entity.GLBalance;
import com.example.moneymarket.entity.GLSetup;
import com.example.moneymarket.exception.BusinessException;
import com.example.moneymarket.repository.GLBalanceRepository;
import com.example.moneymarket.repository.GLSetupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for Batch Job 7: Financial Reports Generation
 * Generates Trial Balance and Balance Sheet CSV reports
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FinancialReportsService {

    private final GLBalanceRepository glBalanceRepository;
    private final GLSetupRepository glSetupRepository;
    private final SystemDateService systemDateService;

    @Value("${reports.directory:reports}")
    private String reportsBaseDirectory;

    /**
     * Batch Job 7: Financial Reports Generation
     *
     * Generates:
     * 1. Trial Balance Report (TrialBalance_YYYYMMDD.csv)
     * 2. Balance Sheet Report (BalanceSheet_YYYYMMDD.csv)
     *
     * @param systemDate The system date for reporting
     * @return Map with report file paths
     */
    @Transactional(readOnly = true)
    public Map<String, String> generateFinancialReports(LocalDate systemDate) {
        LocalDate reportDate = systemDate != null ? systemDate : systemDateService.getSystemDate();
        log.info("Starting Batch Job 7: Financial Reports Generation for date: {}", reportDate);

        Map<String, String> result = new HashMap<>();

        try {
            // Create reports directory
            String reportDateStr = reportDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            Path reportDir = createReportDirectory(reportDateStr);

            // Generate Trial Balance Report
            String trialBalancePath = generateTrialBalanceReport(reportDate, reportDir, reportDateStr);
            
            // Generate Balance Sheet Report
            String balanceSheetPath = generateBalanceSheetReport(reportDate, reportDir, reportDateStr);

            // Return structured response
            result.put("success", "true");
            result.put("trialBalancePath", trialBalancePath);
            result.put("balanceSheetPath", balanceSheetPath);
            result.put("reportDate", reportDateStr);
            result.put("message", "Reports generated successfully");

            log.info("Batch Job 7 completed successfully. Reports generated: {}", result);
            return result;

        } catch (Exception e) {
            log.error("Failed to generate financial reports: {}", e.getMessage(), e);
            throw new BusinessException("Financial reports generation failed: " + e.getMessage());
        }
    }

    /**
     * Create report directory for the given date
     */
    private Path createReportDirectory(String reportDateStr) throws IOException {
        Path reportDir = Paths.get(reportsBaseDirectory, reportDateStr);

        if (!Files.exists(reportDir)) {
            Files.createDirectories(reportDir);
            log.info("Created report directory: {}", reportDir);
        }

        return reportDir;
    }

    /**
     * Generate Trial Balance Report
     *
     * Format:
     * GL_Code, GL_Name, Opening_Bal, DR_Summation, CR_Summation, Closing_Bal
     *
     * Footer row: "TOTAL", sum(Opening_Bal), sum(DR_Summation), sum(CR_Summation), sum(Closing_Bal)
     * Validation: Total DR_Summation must equal Total CR_Summation
     */
    private String generateTrialBalanceReport(LocalDate reportDate, Path reportDir, String reportDateStr)
            throws IOException {
        String fileName = "TrialBalance_" + reportDateStr + ".csv";
        Path filePath = reportDir.resolve(fileName);

        log.info("Generating Trial Balance Report: {}", filePath);

        // Get only active GL numbers (those used in account creation through sub-products)
        List<String> activeGLNumbers = glSetupRepository.findActiveGLNumbersWithAccounts();
        
        if (activeGLNumbers.isEmpty()) {
            log.warn("No active GL numbers found with accounts");
            // Return all GLs as fallback
            List<GLBalance> glBalances = glBalanceRepository.findByTranDate(reportDate);
            return generateTrialBalanceReportFromBalances(glBalances, filePath, reportDate);
        }
        
        log.info("Found {} active GL numbers with accounts", activeGLNumbers.size());
        
        // Get GL balances only for active GLs
        List<GLBalance> glBalances = glBalanceRepository.findByTranDateAndGlNumIn(reportDate, activeGLNumbers);

        return generateTrialBalanceReportFromBalances(glBalances, filePath, reportDate);
    }

    /**
     * Generate Trial Balance Report from GL balances
     * Extracted method to avoid duplication
     */
    private String generateTrialBalanceReportFromBalances(List<GLBalance> glBalances, Path filePath, LocalDate reportDate)
            throws IOException {
        
        if (glBalances.isEmpty()) {
            log.warn("No GL balances found for date: {}", reportDate);
        }

        // Sort by GL Code
        glBalances.sort(Comparator.comparing(GLBalance::getGlNum));

        try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
            // Write header
            writer.write("GL_Code,GL_Name,Opening_Bal,DR_Summation,CR_Summation,Closing_Bal");
            writer.newLine();

            // Initialize totals
            BigDecimal totalOpeningBal = BigDecimal.ZERO;
            BigDecimal totalDRSummation = BigDecimal.ZERO;
            BigDecimal totalCRSummation = BigDecimal.ZERO;
            BigDecimal totalClosingBal = BigDecimal.ZERO;

            // Write data rows
            for (GLBalance glBalance : glBalances) {
                String glNum = glBalance.getGlNum();
                String glName = getGLName(glNum);

                BigDecimal openingBal = nvl(glBalance.getOpeningBal());
                BigDecimal drSummation = nvl(glBalance.getDrSummation());
                BigDecimal crSummation = nvl(glBalance.getCrSummation());
                BigDecimal closingBal = nvl(glBalance.getClosingBal());

                writer.write(String.format("%s,%s,%s,%s,%s,%s",
                        glNum, glName, openingBal, drSummation, crSummation, closingBal));
                writer.newLine();

                // Accumulate totals
                totalOpeningBal = totalOpeningBal.add(openingBal);
                totalDRSummation = totalDRSummation.add(drSummation);
                totalCRSummation = totalCRSummation.add(crSummation);
                totalClosingBal = totalClosingBal.add(closingBal);
            }

            // Write footer row with totals
            writer.write(String.format("TOTAL,,%s,%s,%s,%s",
                    totalOpeningBal, totalDRSummation, totalCRSummation, totalClosingBal));
            writer.newLine();

            log.info("Trial Balance Report generated: {} GL accounts, Total DR={}, Total CR={}",
                    glBalances.size(), totalDRSummation, totalCRSummation);

            // Validation: Total DR must equal Total CR
            if (totalDRSummation.compareTo(totalCRSummation) != 0) {
                String errorMsg = String.format(
                        "Trial Balance validation failed! Total DR (%s) != Total CR (%s). Difference: %s",
                        totalDRSummation, totalCRSummation, totalDRSummation.subtract(totalCRSummation));
                log.error(errorMsg);
                throw new BusinessException(errorMsg);
            }

            log.info("Trial Balance validation passed: DR = CR = {}", totalDRSummation);
        }

        return filePath.toString();
    }

    /**
     * Generate Balance Sheet Report in Excel Format (Side-by-Side Layout)
     *
     * NEW FORMAT:
     * - Liabilities on left (columns A-D)
     * - Assets on right (columns F-I)
     * - Only includes Balance Sheet items (excludes Income/Expenditure)
     * - Only GLs from sub-products with accounts
     * - Excel (.xlsx) format
     */
    private String generateBalanceSheetReport(LocalDate reportDate, Path reportDir, String reportDateStr)
            throws IOException {
        String fileName = "BalanceSheet_" + reportDateStr + ".xlsx";
        Path filePath = reportDir.resolve(fileName);

        log.info("Generating Balance Sheet Report (Excel): {}", filePath);

        // Get Balance Sheet GL numbers only (excludes Income 14* and Expenditure 24*)
        List<String> balanceSheetGLNumbers = glSetupRepository.findBalanceSheetGLNumbersWithAccounts();
        
        if (balanceSheetGLNumbers.isEmpty()) {
            log.warn("No Balance Sheet GL numbers found with accounts");
            // Create empty workbook as fallback
            createEmptyBalanceSheet(filePath, reportDateStr);
            return filePath.toString();
        }
        
        log.info("Found {} Balance Sheet GL numbers with accounts", balanceSheetGLNumbers.size());
        
        // Get GL balances for Balance Sheet GLs only
        List<GLBalance> glBalances = glBalanceRepository.findByTranDateAndGlNumIn(reportDate, balanceSheetGLNumbers);

        if (glBalances.isEmpty()) {
            log.warn("No GL balances found for Balance Sheet GLs on date: {}", reportDate);
            createEmptyBalanceSheet(filePath, reportDateStr);
            return filePath.toString();
        }

        // Separate Liabilities and Assets
        // Simple classification: All GL starting with '1' are Liabilities, all starting with '2' are Assets
        List<GLBalance> liabilities = glBalances.stream()
                .filter(gl -> gl.getGlNum().startsWith("1"))
                .sorted(Comparator.comparing(GLBalance::getGlNum))
                .collect(Collectors.toList());

        List<GLBalance> assets = glBalances.stream()
                .filter(gl -> gl.getGlNum().startsWith("2"))
                .sorted(Comparator.comparing(GLBalance::getGlNum))
                .collect(Collectors.toList());

        // Generate Excel file with side-by-side layout
        generateBalanceSheetExcel(filePath, reportDateStr, liabilities, assets);

        // Calculate totals for logging
        BigDecimal totalLiabilities = liabilities.stream()
                .map(gl -> nvl(gl.getClosingBal()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalAssets = assets.stream()
                .map(gl -> nvl(gl.getClosingBal()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Balance Sheet Report (Excel): {} Liabilities (Total: {}), {} Assets (Total: {})",
                liabilities.size(), totalLiabilities, assets.size(), totalAssets);

        return filePath.toString();
    }

    /**
     * Generate Balance Sheet Excel file with side-by-side layout
     * Liabilities on left, Assets on right
     */
    private void generateBalanceSheetExcel(Path filePath, String reportDateStr,
                                           List<GLBalance> liabilities, List<GLBalance> assets) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fileOut = new FileOutputStream(filePath.toFile())) {

            Sheet sheet = workbook.createSheet("Balance Sheet");

            // Create cell styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle sectionHeaderStyle = createSectionHeaderStyle(workbook);
            CellStyle columnHeaderStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle totalStyle = createTotalStyle(workbook);
            CellStyle numberStyle = createNumberStyle(workbook);

            int currentRow = 0;

            // Row 0: Title (merged across all columns)
            Row titleRow = sheet.createRow(currentRow++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BALANCE SHEET - " + reportDateStr);
            titleCell.setCellStyle(headerStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 6));

            // Row 1: Empty spacer row
            sheet.createRow(currentRow++);

            // Row 2: Section headers
            Row sectionRow = sheet.createRow(currentRow++);
            Cell leftSectionHeader = sectionRow.createCell(0);
            leftSectionHeader.setCellValue("=== LIABILITIES ===");
            leftSectionHeader.setCellStyle(sectionHeaderStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(2, 2, 0, 2)); // Merge A3:C3

            Cell rightSectionHeader = sectionRow.createCell(4);
            rightSectionHeader.setCellValue("=== ASSETS ===");
            rightSectionHeader.setCellStyle(sectionHeaderStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(2, 2, 4, 6)); // Merge E3:G3

            // Row 3: Column Headers
            Row headerRow = sheet.createRow(currentRow++);

            // Liability headers (columns 0-2: A, B, C)
            createStyledCell(headerRow, 0, "GL_Code", columnHeaderStyle);
            createStyledCell(headerRow, 1, "GL_Name", columnHeaderStyle);
            createStyledCell(headerRow, 2, "Closing_Bal", columnHeaderStyle);

            // Empty column (column 3: D)
            headerRow.createCell(3);

            // Asset headers (columns 4-6: E, F, G)
            createStyledCell(headerRow, 4, "GL_Code", columnHeaderStyle);
            createStyledCell(headerRow, 5, "GL_Name", columnHeaderStyle);
            createStyledCell(headerRow, 6, "Closing_Bal", columnHeaderStyle);

            // Data rows - populate side by side
            int maxRows = Math.max(liabilities.size(), assets.size());
            BigDecimal totalLiabilities = BigDecimal.ZERO;
            BigDecimal totalAssets = BigDecimal.ZERO;

            for (int i = 0; i < maxRows; i++) {
                Row row = sheet.createRow(currentRow++);

                // Liability side (columns 0-2: A, B, C)
                if (i < liabilities.size()) {
                    GLBalance liability = liabilities.get(i);
                    String glName = getGLName(liability.getGlNum());
                    BigDecimal closingBal = nvl(liability.getClosingBal());

                    createStyledCell(row, 0, liability.getGlNum(), dataStyle);
                    createStyledCell(row, 1, glName, dataStyle);
                    createStyledNumericCell(row, 2, closingBal, numberStyle);

                    totalLiabilities = totalLiabilities.add(closingBal);
                }

                // Empty column (column 3: D)
                row.createCell(3);

                // Asset side (columns 4-6: E, F, G)
                if (i < assets.size()) {
                    GLBalance asset = assets.get(i);
                    String glName = getGLName(asset.getGlNum());
                    BigDecimal closingBal = nvl(asset.getClosingBal());

                    createStyledCell(row, 4, asset.getGlNum(), dataStyle);
                    createStyledCell(row, 5, glName, dataStyle);
                    createStyledNumericCell(row, 6, closingBal, numberStyle);

                    totalAssets = totalAssets.add(closingBal);
                }
            }

            // Empty row before totals
            currentRow++;

            // Totals row
            Row totalRow = sheet.createRow(currentRow);
            createStyledCell(totalRow, 0, "TOTAL LIABILITIES", totalStyle);
            createStyledNumericCell(totalRow, 2, totalLiabilities, totalStyle);

            createStyledCell(totalRow, 4, "TOTAL ASSETS", totalStyle);
            createStyledNumericCell(totalRow, 6, totalAssets, totalStyle);

            // Set column widths
            sheet.setColumnWidth(0, 15 * 256); // GL_Code left
            sheet.setColumnWidth(1, 40 * 256); // GL_Name left
            sheet.setColumnWidth(2, 15 * 256); // Closing_Bal left
            sheet.setColumnWidth(3, 2 * 256);  // Separator (narrow)
            sheet.setColumnWidth(4, 15 * 256); // GL_Code right
            sheet.setColumnWidth(5, 40 * 256); // GL_Name right
            sheet.setColumnWidth(6, 15 * 256); // Closing_Bal right

            workbook.write(fileOut);
            log.info("Balance Sheet Excel file created: {}", filePath);
        }
    }

    /**
     * Create empty Balance Sheet when no data available
     */
    private void createEmptyBalanceSheet(Path filePath, String reportDateStr) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fileOut = new FileOutputStream(filePath.toFile())) {
            
            Sheet sheet = workbook.createSheet("Balance Sheet");
            Row titleRow = sheet.createRow(0);
            titleRow.createCell(0).setCellValue("BALANCE SHEET - " + reportDateStr);
            Row messageRow = sheet.createRow(2);
            messageRow.createCell(0).setCellValue("No Balance Sheet data available for this date.");
            
            workbook.write(fileOut);
        }
    }

    // Excel style helper methods
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createSectionHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        return style;
    }

    private CellStyle createTotalStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setBorderTop(BorderStyle.DOUBLE);
        return style;
    }

    private CellStyle createNumberStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0.00"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    private void createStyledCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void createStyledNumericCell(Row row, int column, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value.doubleValue());
        cell.setCellStyle(style);
    }

    /**
     * Write a section of the balance sheet and return the total
     */
    private BigDecimal writeBalanceSheetSection(BufferedWriter writer, String category,
                                                List<GLBalance> glBalances) throws IOException {
        BigDecimal total = BigDecimal.ZERO;

        // Sort by GL Code
        glBalances.sort(Comparator.comparing(GLBalance::getGlNum));

        for (GLBalance glBalance : glBalances) {
            String glNum = glBalance.getGlNum();
            String glName = getGLName(glNum);
            BigDecimal closingBal = nvl(glBalance.getClosingBal());

            writer.write(String.format("%s,%s,%s,%s", category, glNum, glName, closingBal));
            writer.newLine();

            total = total.add(closingBal);
        }

        return total;
    }

    /**
     * Filter GL balances by GL number prefix
     */
    private List<GLBalance> filterGLsByPrefix(List<GLBalance> glBalances, String prefix) {
        return glBalances.stream()
                .filter(gl -> gl.getGlNum().startsWith(prefix))
                .collect(Collectors.toList());
    }

    /**
     * Filter GL balances excluding a specific prefix
     */
    private List<GLBalance> filterGLsExcluding(List<GLBalance> glBalances, String excludePrefix) {
        return glBalances.stream()
                .filter(gl -> !gl.getGlNum().startsWith(excludePrefix))
                .collect(Collectors.toList());
    }

    /**
     * Get GL name from GL setup
     */
    private String getGLName(String glNum) {
        Optional<GLSetup> glSetupOpt = glSetupRepository.findById(glNum);
        return glSetupOpt.map(GLSetup::getGlName).orElse("Unknown GL");
    }

    /**
     * Null-safe BigDecimal conversion
     */
    private BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    /**
     * Read CSV file as byte array for download
     *
     * @param filePath The absolute path to the CSV file
     * @return byte array containing file content
     * @throws IOException if file cannot be read
     */
    public byte[] readCsvFileAsBytes(String filePath) throws IOException {
        try {
            Path path = Paths.get(filePath);
            
            if (!Files.exists(path)) {
                throw new IOException("Report file not found: " + filePath);
            }
            
            byte[] content = Files.readAllBytes(path);
            log.debug("Successfully read file: {} ({} bytes)", filePath, content.length);
            return content;
            
        } catch (IOException e) {
            log.error("Failed to read file: {}", filePath, e);
            throw e;
        }
    }
}
