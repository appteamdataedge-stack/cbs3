package com.example.moneymarket.service;

import com.example.moneymarket.dto.AccountDetailsDTO;
import com.example.moneymarket.dto.AccountOptionDTO;
import com.example.moneymarket.entity.*;
import com.example.moneymarket.exception.BusinessException;
import com.example.moneymarket.exception.ResourceNotFoundException;
import com.example.moneymarket.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for Statement of Accounts (SOA) generation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StatementOfAccountsService {

    private final TxnHistAcctRepository txnHistAcctRepository;
    private final CustAcctMasterRepository custAcctMasterRepository;
    private final OFAcctMasterRepository ofAcctMasterRepository;
    private final AcctBalRepository acctBalRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

    /**
     * Generate Statement of Accounts as Excel file
     *
     * @param accountNo Account number
     * @param fromDate Start date
     * @param toDate End date
     * @param format File format (currently only "excel" supported)
     * @return Excel file as byte array
     */
    @Transactional(readOnly = true)
    public byte[] generateStatementOfAccounts(String accountNo, LocalDate fromDate, LocalDate toDate, String format) {
        log.info("Generating SOA for account {} from {} to {}", accountNo, fromDate, toDate);

        // Step 1: Validate inputs
        validateSOARequest(accountNo, fromDate, toDate);

        // Step 2: Get account details
        AccountDetailsDTO accountDetails = getAccountDetails(accountNo);

        // Step 3: Get opening balance
        BigDecimal openingBalance = getOpeningBalance(accountNo, fromDate);

        // Step 4: Query all transactions in date range
        List<TxnHistAcct> transactions = txnHistAcctRepository
                .findByAccNoAndTranDateBetweenOrderByTranDateAscRcreTimeAsc(accountNo, fromDate, toDate);

        log.info("Found {} transactions for account {} in date range", transactions.size(), accountNo);

        // Step 5: Calculate closing balance
        BigDecimal closingBalance = transactions.isEmpty() 
                ? openingBalance 
                : transactions.get(transactions.size() - 1).getBalanceAfterTran();

        // Step 6: Generate Excel file
        try {
            return generateExcelSOA(accountDetails, openingBalance, closingBalance, transactions, fromDate, toDate);
        } catch (IOException e) {
            log.error("Error generating Excel SOA for account {}: {}", accountNo, e.getMessage(), e);
            throw new BusinessException("Failed to generate statement: " + e.getMessage());
        }
    }

    /**
     * Validate SOA request parameters
     */
    private void validateSOARequest(String accountNo, LocalDate fromDate, LocalDate toDate) {
        if (accountNo == null || accountNo.trim().isEmpty()) {
            throw new IllegalArgumentException("Account number is required");
        }

        if (fromDate == null) {
            throw new IllegalArgumentException("From date is required");
        }

        if (toDate == null) {
            throw new IllegalArgumentException("To date is required");
        }

        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("From date must be before or equal to To date");
        }

        if (fromDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("From date cannot be in the future");
        }

        if (toDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("To date cannot be in the future");
        }

        // Check 6-month maximum range
        long monthsBetween = ChronoUnit.MONTHS.between(fromDate, toDate);
        if (monthsBetween > 6) {
            throw new IllegalArgumentException("Date range cannot exceed 6 months");
        }

        // Verify account exists
        if (!accountExists(accountNo)) {
            throw new ResourceNotFoundException("Account", "Account Number", accountNo);
        }
    }

    /**
     * Check if account exists (customer or office)
     */
    private boolean accountExists(String accountNo) {
        return custAcctMasterRepository.existsById(accountNo) || ofAcctMasterRepository.existsById(accountNo);
    }

    /**
     * Get account details for SOA header
     */
    private AccountDetailsDTO getAccountDetails(String accountNo) {
        // Try customer account first
        Optional<CustAcctMaster> custAccount = custAcctMasterRepository.findById(accountNo);
        if (custAccount.isPresent()) {
            CustAcctMaster account = custAccount.get();
            String customerName = "";
            if (account.getCustomer() != null) {
                CustMaster customer = account.getCustomer();
                // Build customer name from firstName + lastName or tradeName
                if (customer.getTradeName() != null && !customer.getTradeName().isEmpty()) {
                    customerName = customer.getTradeName();
                } else {
                    customerName = (customer.getFirstName() != null ? customer.getFirstName() : "") + 
                                   " " + (customer.getLastName() != null ? customer.getLastName() : "");
                    customerName = customerName.trim();
                }
            }
            String productName = "";
            String glNum = "";

            if (account.getSubProduct() != null) {
                SubProdMaster subProduct = account.getSubProduct();
                glNum = subProduct.getCumGLNum();
                if (subProduct.getProduct() != null) {
                    productName = subProduct.getProduct().getProductName();
                }
            }

            return AccountDetailsDTO.builder()
                    .accountNo(accountNo)
                    .accountName(account.getAcctName())
                    .customerName(customerName)
                    .accountType("Customer")
                    .glNum(glNum)
                    .productName(productName)
                    .branchId(account.getBranchCode())
                    .build();
        }

        // Try office account
        Optional<OFAcctMaster> officeAccount = ofAcctMasterRepository.findById(accountNo);
        if (officeAccount.isPresent()) {
            OFAcctMaster account = officeAccount.get();
            String productName = "";
            String glNum = "";

            if (account.getSubProduct() != null) {
                SubProdMaster subProduct = account.getSubProduct();
                glNum = subProduct.getCumGLNum();
                if (subProduct.getProduct() != null) {
                    productName = subProduct.getProduct().getProductName();
                }
            }

            return AccountDetailsDTO.builder()
                    .accountNo(accountNo)
                    .accountName(account.getAcctName())
                    .customerName(null)
                    .accountType("Office")
                    .glNum(glNum)
                    .productName(productName)
                    .branchId(account.getBranchCode())
                    .build();
        }

        throw new ResourceNotFoundException("Account", "Account Number", accountNo);
    }

    /**
     * Get opening balance for the statement period
     */
    private BigDecimal getOpeningBalance(String accountNo, LocalDate fromDate) {
        // Try to get last transaction before fromDate
        Optional<TxnHistAcct> lastTransaction = txnHistAcctRepository.findLastTransactionBeforeDate(accountNo, fromDate);
        if (lastTransaction.isPresent()) {
            return lastTransaction.get().getBalanceAfterTran();
        }

        // 3-tier fallback: Try previous day from acct_bal
        Optional<AcctBal> previousDayBalance = acctBalRepository.findByAccountNoAndTranDate(
                accountNo, fromDate.minusDays(1));
        if (previousDayBalance.isPresent()) {
            return previousDayBalance.get().getCurrentBalance();
        }

        // Try last transaction date from acct_bal
        // This would require a query to find the most recent acct_bal record
        // For simplicity, we'll default to 0 if no history found
        log.debug("No opening balance found for account {}, defaulting to 0", accountNo);
        return BigDecimal.ZERO;
    }

    /**
     * Generate Excel file for Statement of Accounts
     */
    private byte[] generateExcelSOA(AccountDetailsDTO accountDetails, BigDecimal openingBalance,
                                     BigDecimal closingBalance, List<TxnHistAcct> transactions,
                                     LocalDate fromDate, LocalDate toDate) throws IOException {

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Statement of Account");

            // Create cell styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle columnHeaderStyle = createColumnHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle amountStyle = createAmountStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);
            CellStyle boldStyle = createBoldStyle(workbook);

            int rowNum = 0;

            // Write header section
            rowNum = writeHeaderSection(sheet, accountDetails, fromDate, toDate, openingBalance, 
                    headerStyle, boldStyle, rowNum);

            // Write column headers
            rowNum = writeColumnHeaders(sheet, columnHeaderStyle, rowNum);

            // Write data rows
            rowNum = writeDataRows(sheet, transactions, dataStyle, amountStyle, dateStyle, rowNum);

            // Write footer section
            writeFooterSection(sheet, openingBalance, closingBalance, transactions, boldStyle, amountStyle, rowNum);

            // Auto-size columns
            for (int i = 0; i < 7; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000); // Add padding
            }

            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * Write header section of the Excel file
     */
    private int writeHeaderSection(Sheet sheet, AccountDetailsDTO accountDetails, LocalDate fromDate,
                                    LocalDate toDate, BigDecimal openingBalance, CellStyle headerStyle,
                                    CellStyle boldStyle, int rowNum) {
        // Row 1: Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("STATEMENT OF ACCOUNT");
        titleCell.setCellStyle(headerStyle);

        // Row 2: Empty
        rowNum++;

        // Row 3: Account Number
        Row acctNoRow = sheet.createRow(rowNum++);
        Cell acctNoLabelCell = acctNoRow.createCell(0);
        acctNoLabelCell.setCellValue("Account Number:");
        acctNoLabelCell.setCellStyle(boldStyle);
        Cell acctNoValueCell = acctNoRow.createCell(1);
        acctNoValueCell.setCellValue(accountDetails.getAccountNo());

        // Row 4: Account Name
        Row acctNameRow = sheet.createRow(rowNum++);
        Cell acctNameLabelCell = acctNameRow.createCell(0);
        acctNameLabelCell.setCellValue("Account Name:");
        acctNameLabelCell.setCellStyle(boldStyle);
        Cell acctNameValueCell = acctNameRow.createCell(1);
        acctNameValueCell.setCellValue(accountDetails.getAccountName());

        // Row 5: Customer Name (if customer account)
        if (accountDetails.getCustomerName() != null && !accountDetails.getCustomerName().isEmpty()) {
            Row custNameRow = sheet.createRow(rowNum++);
            Cell custNameLabelCell = custNameRow.createCell(0);
            custNameLabelCell.setCellValue("Customer Name:");
            custNameLabelCell.setCellStyle(boldStyle);
            Cell custNameValueCell = custNameRow.createCell(1);
            custNameValueCell.setCellValue(accountDetails.getCustomerName());
        }

        // Row 6: Account Type
        Row acctTypeRow = sheet.createRow(rowNum++);
        Cell acctTypeLabelCell = acctTypeRow.createCell(0);
        acctTypeLabelCell.setCellValue("Account Type:");
        acctTypeLabelCell.setCellStyle(boldStyle);
        Cell acctTypeValueCell = acctTypeRow.createCell(1);
        acctTypeValueCell.setCellValue(accountDetails.getAccountType());

        // Row 7: Period
        Row periodRow = sheet.createRow(rowNum++);
        Cell periodLabelCell = periodRow.createCell(0);
        periodLabelCell.setCellValue("Period:");
        periodLabelCell.setCellStyle(boldStyle);
        Cell periodValueCell = periodRow.createCell(1);
        periodValueCell.setCellValue(fromDate.format(DATE_FORMATTER) + " to " + toDate.format(DATE_FORMATTER));

        // Row 8: Opening Balance
        Row openingBalRow = sheet.createRow(rowNum++);
        Cell openingBalLabelCell = openingBalRow.createCell(0);
        openingBalLabelCell.setCellValue("Opening Balance:");
        openingBalLabelCell.setCellStyle(boldStyle);
        Cell openingBalValueCell = openingBalRow.createCell(1);
        openingBalValueCell.setCellValue(formatAmount(openingBalance));

        // Row 9: Empty
        rowNum++;

        return rowNum;
    }

    /**
     * Write column headers
     */
    private int writeColumnHeaders(Sheet sheet, CellStyle columnHeaderStyle, int rowNum) {
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"Date", "Value Date", "Transaction ID", "Narration", "Debit", "Credit", "Balance"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(columnHeaderStyle);
        }
        return rowNum;
    }

    /**
     * Write data rows
     */
    private int writeDataRows(Sheet sheet, List<TxnHistAcct> transactions, CellStyle dataStyle,
                               CellStyle amountStyle, CellStyle dateStyle, int rowNum) {
        for (TxnHistAcct transaction : transactions) {
            Row dataRow = sheet.createRow(rowNum++);

            // Date
            Cell dateCell = dataRow.createCell(0);
            dateCell.setCellValue(transaction.getTranDate().format(DATE_FORMATTER));
            dateCell.setCellStyle(dateStyle);

            // Value Date
            Cell valueDateCell = dataRow.createCell(1);
            valueDateCell.setCellValue(transaction.getValueDate().format(DATE_FORMATTER));
            valueDateCell.setCellStyle(dateStyle);

            // Transaction ID
            Cell tranIdCell = dataRow.createCell(2);
            tranIdCell.setCellValue(transaction.getTranId());
            tranIdCell.setCellStyle(dataStyle);

            // Narration
            Cell narrationCell = dataRow.createCell(3);
            narrationCell.setCellValue(transaction.getNarration() != null ? transaction.getNarration() : "");
            narrationCell.setCellStyle(dataStyle);

            // Debit
            Cell debitCell = dataRow.createCell(4);
            if (transaction.getTranType() == TxnHistAcct.TransactionType.D) {
                debitCell.setCellValue(formatAmount(transaction.getTranAmt()));
            } else {
                debitCell.setCellValue("");
            }
            debitCell.setCellStyle(amountStyle);

            // Credit
            Cell creditCell = dataRow.createCell(5);
            if (transaction.getTranType() == TxnHistAcct.TransactionType.C) {
                creditCell.setCellValue(formatAmount(transaction.getTranAmt()));
            } else {
                creditCell.setCellValue("");
            }
            creditCell.setCellStyle(amountStyle);

            // Balance (BALANCE_AFTER_TRAN)
            Cell balanceCell = dataRow.createCell(6);
            balanceCell.setCellValue(formatAmount(transaction.getBalanceAfterTran()));
            balanceCell.setCellStyle(amountStyle);
        }
        return rowNum;
    }

    /**
     * Write footer section
     */
    private void writeFooterSection(Sheet sheet, BigDecimal openingBalance, BigDecimal closingBalance,
                                     List<TxnHistAcct> transactions, CellStyle boldStyle,
                                     CellStyle amountStyle, int rowNum) {
        // Calculate totals
        BigDecimal totalDebits = transactions.stream()
                .filter(t -> t.getTranType() == TxnHistAcct.TransactionType.D)
                .map(TxnHistAcct::getTranAmt)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredits = transactions.stream()
                .filter(t -> t.getTranType() == TxnHistAcct.TransactionType.C)
                .map(TxnHistAcct::getTranAmt)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Empty row
        rowNum++;

        // Closing Balance
        Row closingBalRow = sheet.createRow(rowNum++);
        Cell closingBalLabelCell = closingBalRow.createCell(0);
        closingBalLabelCell.setCellValue("Closing Balance:");
        closingBalLabelCell.setCellStyle(boldStyle);
        Cell closingBalValueCell = closingBalRow.createCell(1);
        closingBalValueCell.setCellValue(formatAmount(closingBalance));
        closingBalValueCell.setCellStyle(amountStyle);

        // Total Debits
        Row totalDebitsRow = sheet.createRow(rowNum++);
        Cell totalDebitsLabelCell = totalDebitsRow.createCell(0);
        totalDebitsLabelCell.setCellValue("Total Debits:");
        totalDebitsLabelCell.setCellStyle(boldStyle);
        Cell totalDebitsValueCell = totalDebitsRow.createCell(1);
        totalDebitsValueCell.setCellValue(formatAmount(totalDebits));
        totalDebitsValueCell.setCellStyle(amountStyle);

        // Total Credits
        Row totalCreditsRow = sheet.createRow(rowNum++);
        Cell totalCreditsLabelCell = totalCreditsRow.createCell(0);
        totalCreditsLabelCell.setCellValue("Total Credits:");
        totalCreditsLabelCell.setCellStyle(boldStyle);
        Cell totalCreditsValueCell = totalCreditsRow.createCell(1);
        totalCreditsValueCell.setCellValue(formatAmount(totalCredits));
        totalCreditsValueCell.setCellStyle(amountStyle);
    }

    /**
     * Get list of all accounts for dropdown
     */
    @Transactional(readOnly = true)
    public List<AccountOptionDTO> getAccountList() {
        List<AccountOptionDTO> accounts = new ArrayList<>();

        // Get all customer accounts
        List<CustAcctMaster> customerAccounts = custAcctMasterRepository.findAll();
        accounts.addAll(customerAccounts.stream()
                .map(account -> AccountOptionDTO.builder()
                        .accountNo(account.getAccountNo())
                        .accountName(account.getAcctName())
                        .accountType("Customer")
                        .build())
                .collect(Collectors.toList()));

        // Get all office accounts
        List<OFAcctMaster> officeAccounts = ofAcctMasterRepository.findAll();
        accounts.addAll(officeAccounts.stream()
                .map(account -> AccountOptionDTO.builder()
                        .accountNo(account.getAccountNo())
                        .accountName(account.getAcctName())
                        .accountType("Office")
                        .build())
                .collect(Collectors.toList()));

        // Sort by account number
        accounts.sort((a, b) -> a.getAccountNo().compareTo(b.getAccountNo()));

        return accounts;
    }

    /**
     * Format amount with 2 decimal places
     */
    private String formatAmount(BigDecimal amount) {
        return String.format("%,.2f", amount);
    }

    // Cell style creation methods
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createColumnHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
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

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createAmountStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createBoldStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }
}

