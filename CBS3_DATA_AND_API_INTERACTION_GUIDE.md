# CBS3 Data & API Interaction Guide

This document maps the CBS3 front-end pages to their backing REST APIs and captures how those APIs persist or query data in MySQL. It is meant to serve as a field guide while certifying fixes or extending functionality.

## 1. Architectural Snapshot
- **Frontend**: React 19 + Vite, Material UI, TanStack Query for data fetching.
- **Backend**: Spring Boot 3.1, layered controllers → services → repositories with JPA over MySQL.
- **Database**: Core banking schema defined in `moneydb041125.sql` under `db backup/` and incremental migration scripts at repo root.
- **Environment**: Frontend pulls `VITE_API_URL` (defaults to `http://localhost:8082/api`) and sends JSON requests with bearer tokens and optional CSRF headers. Errors are normalized before surfacing to the UI.

```11:122:/workspace/frontend/src/api/apiClient.ts
const API_BASE_URL = `${import.meta.env.VITE_API_URL || 'http://localhost:8082'}/api`;
...
apiClient.interceptors.request.use(
  (config) => {
    const token = getAuthToken();
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    if (config.method !== 'get') {
      const csrfToken = getCsrfToken();
      if (csrfToken) {
        config.headers[CSRF_HEADER] = csrfToken;
      }
    }
    const timestamp = new Date().getTime();
    config.params = { ...(config.params || {}), _t: timestamp };
    return config;
  },
...
```

## 2. Feature Modules

Each subsection links the visible React page(s) to API endpoints and enumerates the tables touched by common actions. Maker–checker flows follow a “create/update → verify” pattern that toggles `Verifier` columns instead of duplicating rows.

### 2.1 Customer Management (`/customers`)

| UI Flow | REST Endpoint | Service Layer | Tables Read/Written | Notes |
| --- | --- | --- | --- | --- |
| List/search | `GET /api/customers?page=…&size=…&search=` | `CustomerService.getAllCustomers` | `Cust_Master` | Paging handled by Spring `Pageable`. |
| View | `GET /api/customers/{id}` | `CustomerService.getCustomer` | `Cust_Master` | Throws `ResourceNotFoundException` if absent. |
| Create | `POST /api/customers` | `CustomerService.createCustomer` | **Insert** `Cust_Master` | External ID uniqueness, type-driven validations, customer ID generation. |
| Update | `PUT /api/customers/{id}` | `CustomerService.updateCustomer` | **Update** `Cust_Master` | Resets maker–checker fields on edit. |
| Verify | `POST /api/customers/{id}/verify` | `CustomerService.verifyCustomer` | **Update** `Cust_Master` | Maker and verifier must differ. |

```37:191:/workspace/moneymarket/src/main/java/com/example/moneymarket/service/CustomerService.java
@Transactional
public CustomerResponseDTO createCustomer(CustomerRequestDTO customerRequestDTO) {
    if (customerRequestDTO.getExtCustId() != null &&
        custMasterRepository.existsByExtCustId(customerRequestDTO.getExtCustId())) {
        throw new BusinessException("External Customer ID already exists");
    }
    Integer custId = customerIdService.generateCustomerId(customerRequestDTO.getCustType());
    CustMaster customer = mapToEntity(customerRequestDTO);
    customer.setCustId(custId);
    customer.setEntryDate(LocalDate.now());
    customer.setEntryTime(LocalTime.now());
    CustMaster savedCustomer = custMasterRepository.save(customer);
    ...
}
```

**Table impact**: Rows in `Cust_Master` receive the generated `Cust_Id`, audit fields (`Entry_Date`, `Entry_Time`, `Maker_Id`) and are later stamped with `Verifier_Id`, `Verification_Date`, `Verification_Time` when verified. Customer IDs are prefixed by type (`CustomerIdService.generateCustomerId`).

### 2.2 Product Management (`/products`)

| UI Flow | REST Endpoint | Service Layer | Tables |
| --- | --- | --- | --- |
| List/search/detail | `GET /api/products` & `GET /api/products/{id}` | `ProductService.getAllProducts`/`getProduct` | `Prod_Master` |
| Create | `POST /api/products` | `ProductService.createProduct` | **Insert** `Prod_Master` |
| Update | `PUT /api/products/{id}` | `ProductService.updateProduct` | **Update** `Prod_Master` |
| Verify | `POST /api/products/{id}/verify` | `ProductService.verifyProduct` | **Update** `Prod_Master` |
| GL dropdown | `GET /api/products/gl-options` | `GLSetupService.getGLSetupsByLayerId(3)` | `GL_Setup` |

```44:177:/workspace/moneymarket/src/main/java/com/example/moneymarket/service/ProductService.java
@Transactional
public ProductResponseDTO createProduct(ProductRequestDTO productRequestDTO) {
    if (prodMasterRepository.existsByProductCode(productRequestDTO.getProductCode())) {
        throw new BusinessException("Product Code already exists");
    }
    glNumberService.validateGLNumber(productRequestDTO.getCumGLNum(), null, 3);
    ProdMaster product = mapToEntity(productRequestDTO);
    product.setEntryDate(systemDateService.getSystemDate());
    product.setEntryTime(LocalTime.now());
    ProdMaster savedProduct = prodMasterRepository.save(product);
    return mapToResponse(savedProduct);
}
```

**Row impact**: `Prod_Master` stores maker, verification, GL linkage (`Cum_GL_Num`), and flags (`Customer_Product_Flag`, `Interest_Bearing_Flag`). Verification populates the `Verifier` columns; edits clear them to enforce re-approval.

### 2.3 Sub-Product Management (`/subproducts`)

| UI Flow | Endpoint | Service | Tables |
| --- | --- | --- | --- |
| List/detail | `GET /api/subproducts` / `{id}` | `SubProductService.getAllSubProducts` | `SubProd_Master` (+ `Prod_Master` via relationship) |
| Create | `POST /api/subproducts` | `SubProductService.createSubProduct` | **Insert** `SubProd_Master`, optional interest config |
| Update | `PUT /api/subproducts/{id}` | `SubProductService.updateSubProduct` | **Update** `SubProd_Master` |
| Verify | `POST /api/subproducts/{id}/verify` | `SubProductService.verifySubProduct` | **Update** `SubProd_Master` |
| GL dropdowns | `/gl-options`, `/gl-options/{parent}` | `GLSetupService` | `GL_Setup` |

```52:333:/workspace/moneymarket/src/main/java/com/example/moneymarket/service/SubProductService.java
public SubProductResponseDTO createSubProduct(SubProductRequestDTO dto) {
    if (subProdMasterRepository.existsBySubProductCode(dto.getSubProductCode())) {
        throw new BusinessException("Sub-Product Code already exists");
    }
    ProdMaster product = prodMasterRepository.findById(dto.getProductId())
            .orElseThrow(() -> new ResourceNotFoundException("Product", "ID", dto.getProductId()));
    glNumberService.validateGLNumber(dto.getCumGLNum(), product.getCumGLNum(), 4);
    SubProdMaster subProduct = mapToEntity(dto, product);
    applyEffectiveInterestRate(product, subProduct);
    SubProdMaster saved = subProdMasterRepository.save(subProduct);
    return mapToResponse(saved);
}
```

**Row impact**: `SubProd_Master` rows carry customer/office applicability, GL mapping, interest GLs, and derived `Effective_Interest_Rate`. Maker–checker mirrors product behavior. GL balances (`GL_Balance`) are consulted on updates to prevent deactivation with non-zero balances.

### 2.4 Customer Account Management (`/accounts`)

| UI Flow | Endpoint | Service | Tables Impacted |
| --- | --- | --- | --- |
| List/detail | `GET /api/accounts/customer` / `{accountNo}` | `CustomerAccountService.getAllAccounts`/`getAccount` | `Cust_Acct_Master`, `Acct_Bal` (latest), `Acct_Bal_Accrual`, `Tran_Table` aggregates |
| Create | `POST /api/accounts/customer` | `CustomerAccountService.createAccount` | **Insert** `Cust_Acct_Master`, **Insert** zero row in `Acct_Bal` |
| Update | `PUT /api/accounts/customer/{accountNo}` | `CustomerAccountService.updateAccount` | **Update** `Cust_Acct_Master` |
| Close | `POST /api/accounts/customer/{accountNo}/close` | `CustomerAccountService.closeAccount` | **Update** `Cust_Acct_Master` (status/date) |

```47:152:/workspace/moneymarket/src/main/java/com/example/moneymarket/service/CustomerAccountService.java
@Transactional
public CustomerAccountResponseDTO createAccount(CustomerAccountRequestDTO dto) {
    CustMaster customer = custMasterRepository.findById(dto.getCustId())
        .orElseThrow(() -> new ResourceNotFoundException("Customer", "ID", dto.getCustId()));
    SubProdMaster subProduct = subProdMasterRepository.findByIdWithProduct(dto.getSubProductId())
        .orElseThrow(() -> new ResourceNotFoundException("Sub-Product", "ID", dto.getSubProductId()));
    applyTenorAndMaturityLogic(dto, subProduct);
    String accountNo = accountNumberService.generateCustomerAccountNumber(customer, subProduct);
    CustAcctMaster savedAccount = custAcctMasterRepository.save(mapToEntity(dto, customer, subProduct, accountNo, subProduct.getCumGLNum()));
    AcctBal accountBalance = AcctBal.builder()
            .tranDate(systemDateService.getSystemDate())
            .accountNo(savedAccount.getAccountNo())
            .currentBalance(BigDecimal.ZERO)
            .availableBalance(BigDecimal.ZERO)
            .lastUpdated(systemDateService.getSystemDateTime())
            .build();
    acctBalRepository.save(accountBalance);
    return mapToResponse(savedAccount, accountBalance);
}
```

**Row impact**:
- `Cust_Acct_Master` receives GL, customer linkage, tenor, loan limit (only if GL starts with “2”), and status.
- `Acct_Bal` gets a day-zero record keyed by `(Tran_Date=System_Date, Account_No)` to support same-day posting.
- Closing an account enforces zero balance before setting `Account_Status=Closed` and `Date_Closure` (system date).

**Balance computation** uses transactional reads from balance, transaction, and accrual tables; available balance includes loan limit on asset accounts.

```143:257:/workspace/moneymarket/src/main/java/com/example/moneymarket/service/BalanceService.java
public AccountBalanceDTO getComputedAccountBalance(String accountNo, LocalDate systemDate) {
    AcctBal currentDayBalance = acctBalRepository.findByAccountNoAndTranDate(accountNo, systemDate)
        .orElseGet(() -> acctBalRepository.findLatestByAccountNo(accountNo)
            .orElseThrow(() -> new ResourceNotFoundException("Account Balance", "Account Number", accountNo)));
    BigDecimal previousDayOpeningBalance = accountBalanceUpdateService.getPreviousDayClosingBalance(accountNo, systemDate);
    BigDecimal dateDebits = tranTableRepository.sumDebitTransactionsForAccountOnDate(accountNo, systemDate).orElse(BigDecimal.ZERO);
    BigDecimal dateCredits = tranTableRepository.sumCreditTransactionsForAccountOnDate(accountNo, systemDate).orElse(BigDecimal.ZERO);
    BigDecimal computedBalance = previousDayOpeningBalance.add(dateCredits).subtract(dateDebits);
    BigDecimal availableBalance = (isAssetAccount) ? previousDayOpeningBalance.add(loanLimit).add(dateCredits).subtract(dateDebits)
                                                   : previousDayOpeningBalance;
    BigDecimal interestAccrued = getLatestInterestAccrued(accountNo);
    return AccountBalanceDTO.builder()
            .accountNo(accountNo)
            .availableBalance(availableBalance)
            .currentBalance(currentDayBalance.getCurrentBalance())
            .todayDebits(dateDebits)
            .todayCredits(dateCredits)
            .computedBalance(computedBalance)
            .interestAccrued(interestAccrued)
            .build();
}
```

### 2.5 Office Account Management (`/office-accounts`)

| UI Flow | Endpoint | Service | Tables |
| --- | --- | --- | --- |
| List/detail | `GET /api/accounts/office` / `{accountNo}` | `OfficeAccountService.getAllAccounts` | `OFAcct_Master` |
| Create | `POST /api/accounts/office` | `OfficeAccountService.createAccount` | **Insert** `OFAcct_Master`, **Insert** zero row in `Acct_Bal` |
| Update | `PUT /api/accounts/office/{accountNo}` | `OfficeAccountService.updateAccount` | **Update** `OFAcct_Master` |
| Close | `POST /api/accounts/office/{accountNo}/close` | `OfficeAccountService.closeAccount` | **Update** `OFAcct_Master` |

```42:168:/workspace/moneymarket/src/main/java/com/example/moneymarket/service/OfficeAccountService.java
public OfficeAccountResponseDTO createAccount(OfficeAccountRequestDTO dto) {
    SubProdMaster subProduct = subProdMasterRepository.findById(dto.getSubProductId())
        .orElseThrow(() -> new ResourceNotFoundException("Sub-Product", "ID", dto.getSubProductId()));
    String accountNo = accountNumberService.generateOfficeAccountNumber(subProduct.getCumGLNum());
    OFAcctMaster savedAccount = ofAcctMasterRepository.save(mapToEntity(dto, subProduct, accountNo, subProduct.getCumGLNum()));
    AcctBal accountBalance = AcctBal.builder()
        .tranDate(systemDateService.getSystemDate())
        .accountNo(savedAccount.getAccountNo())
        .currentBalance(BigDecimal.ZERO)
        .availableBalance(BigDecimal.ZERO)
        .lastUpdated(systemDateService.getSystemDateTime())
        .build();
    acctBalRepository.save(accountBalance);
    return mapToResponse(savedAccount);
}
```

Office account numbers follow a “9 + GL + 2-digit sequence” format to avoid clashing with customer numbers.

```107:145:/workspace/moneymarket/src/main/java/com/example/moneymarket/service/AccountNumberService.java
public String generateOfficeAccountNumber(String glNum) {
    AccountSeq accountSeq = accountSeqRepository.findByGlNumWithLock(glNum)
        .orElseGet(() -> new AccountSeq(glNum, 0, LocalDateTime.now()));
    int nextSequence = accountSeq.getSeqNumber() + 1;
    if (nextSequence > 99) {
        throw new BusinessException("Office account number sequence for GL " + glNum + " has reached its maximum (99)");
    }
    accountSeq.setSeqNumber(nextSequence);
    accountSeqRepository.save(accountSeq);
    return "9" + glNum + String.format("%02d", nextSequence);
}
```

### 2.6 Transaction Lifecycle (`/transactions`)

| Stage | Endpoint | Service Method | Primary Table Mutations |
| --- | --- | --- | --- |
| Entry (Maker) | `POST /api/transactions/entry` | `TransactionService.createTransaction` | Inserts rows into `Tran_Table` (status `Entry`, separate row per leg). |
| Post (Checker) | `POST /api/transactions/{tranId}/post` | `TransactionService.postTransaction` | Updates `Tran_Table.Tran_Status → Posted`, adjusts `Acct_Bal` (`updateAccountBalance`), inserts `GL_Movement`, updates `GL_Balance`. |
| Verify (Authorizer) | `POST /api/transactions/{tranId}/verify` | `TransactionService.verifyTransaction` | Updates `Tran_Table.Tran_Status → Verified`, writes to `Txn_Hist_Acct` via `TransactionHistoryService`. |
| Reverse | `POST /api/transactions/{tranId}/reverse` | `TransactionService.reverseTransaction` | Inserts opposite-sign `Tran_Table` rows (auto-verified), applies balancing adjustments to `Acct_Bal` and `GL_Balance`, logs `GL_Movement`. |

```61:218:/workspace/moneymarket/src/main/java/com/example/moneymarket/service/TransactionService.java
public TransactionResponseDTO createTransaction(TransactionRequestDTO dto) {
    validateTransactionBalance(dto);
    String tranId = generateTransactionId();
    LocalDate tranDate = systemDateService.getSystemDate();
    List<TranTable> transactions = new ArrayList<>();
    int lineNumber = 1;
    for (TransactionLineDTO lineDTO : dto.getLines()) {
        UnifiedAccountService.AccountInfo accountInfo = unifiedAccountService.getAccountInfo(lineDTO.getAccountNo());
        TranTable transaction = TranTable.builder()
                .tranId(tranId + "-" + lineNumber++)
                .tranDate(tranDate)
                .valueDate(dto.getValueDate())
                .drCrFlag(lineDTO.getDrCrFlag())
                .tranStatus(TranStatus.Entry)
                .accountNo(lineDTO.getAccountNo())
                .lcyAmt(lineDTO.getLcyAmt())
                .build();
        transactions.add(transaction);
    }
    tranTableRepository.saveAll(transactions);
    return buildTransactionResponse(tranId, tranDate, dto.getValueDate(), dto.getNarration(), transactions);
}

public TransactionResponseDTO postTransaction(String tranId) {
    List<TranTable> transactions = tranTableRepository.findAll().stream()
        .filter(t -> t.getTranId().startsWith(tranId + "-") && t.getTranStatus() == TranStatus.Entry)
        .collect(Collectors.toList());
    transactions.forEach(t -> t.setTranStatus(TranStatus.Posted));
    for (TranTable transaction : transactions) {
        String glNum = unifiedAccountService.getGlNum(transaction.getAccountNo());
        validationService.updateAccountBalanceForTransaction(transaction.getAccountNo(), transaction.getDrCrFlag(), transaction.getLcyAmt());
        BigDecimal newGLBalance = balanceService.updateGLBalance(glNum, transaction.getDrCrFlag(), transaction.getLcyAmt());
        GLMovement glMovement = GLMovement.builder()
                .transaction(transaction)
                .glSetup(glSetupRepository.findById(glNum).orElseThrow(...))
                .drCrFlag(transaction.getDrCrFlag())
                .amount(transaction.getLcyAmt())
                .balanceAfter(newGLBalance)
                .build();
        glMovements.add(glMovement);
    }
    tranTableRepository.saveAll(transactions);
    glMovementRepository.saveAll(glMovements);
    return buildTransactionResponse(...);
}
```

Transaction verification persists running balances into `Txn_Hist_Acct`, enabling the Statement of Accounts report.

```39:111:/workspace/moneymarket/src/main/java/com/example/moneymarket/service/TransactionHistoryService.java
public void createTransactionHistory(TranTable transaction, String verifierUserId) {
    AccountDetails accountDetails = getAccountDetails(transaction.getAccountNo());
    BigDecimal openingBalance = getOpeningBalanceForTransaction(transaction.getAccountNo(), transaction.getTranDate());
    BigDecimal tranAmt = transaction.getLcyAmt();
    TxnHistAcct.TransactionType tranType = transaction.getDrCrFlag() == TranTable.DrCrFlag.D ? TxnHistAcct.TransactionType.D : TxnHistAcct.TransactionType.C;
    BigDecimal balanceAfterTran = (tranType == TxnHistAcct.TransactionType.C)
            ? openingBalance.add(tranAmt)
            : openingBalance.subtract(tranAmt);
    TxnHistAcct histRecord = new TxnHistAcct();
    histRecord.setAccNo(transaction.getAccountNo());
    histRecord.setTranId(transaction.getTranId());
    histRecord.setTranDate(transaction.getTranDate());
    histRecord.setTranAmt(tranAmt);
    histRecord.setOpeningBalance(openingBalance);
    histRecord.setBalanceAfterTran(balanceAfterTran);
    txnHistAcctRepository.save(histRecord);
}
```

### 2.7 Statement of Accounts (`/statement-of-accounts`)

- UI fetches account options by calling `GET /api/soa/accounts` (merges customer & office accounts).
- Generation triggers `POST /api/soa/generate?accountNo=X&fromDate=Y&toDate=Z&format=excel`, which streams an XLSX file.
- Date ranges are validated client-side (≤ 6 months) and server-side via `/api/soa/validate-date-range`.

```41:151:/workspace/moneymarket/src/main/java/com/example/moneymarket/controller/StatementOfAccountsController.java
@PostMapping("/generate")
public ResponseEntity<?> generateSOA(@RequestParam String accountNo,
                                     @RequestParam LocalDate fromDate,
                                     @RequestParam LocalDate toDate,
                                     @RequestParam(defaultValue = "excel") String format) {
    byte[] fileBytes = soaService.generateStatementOfAccounts(accountNo, fromDate, toDate, format);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    headers.setContentDispositionFormData("attachment", String.format("SOA_%s_%s_to_%s.xlsx", accountNo, fromDate, toDate));
    return ResponseEntity.ok().headers(headers).body(fileBytes);
}
```

`StatementOfAccountsService` composes the spreadsheet by reading `Txn_Hist_Acct`, `Acct_Bal`, `Cust_Acct_Master`/`OFAcct_Master`, formatting opening/closing balances, and deriving totals.

```51:268:/workspace/moneymarket/src/main/java/com/example/moneymarket/service/StatementOfAccountsService.java
public byte[] generateStatementOfAccounts(String accountNo, LocalDate fromDate, LocalDate toDate, String format) {
    validateSOARequest(accountNo, fromDate, toDate);
    AccountDetailsDTO accountDetails = getAccountDetails(accountNo);
    BigDecimal openingBalance = getOpeningBalance(accountNo, fromDate);
    List<TxnHistAcct> transactions = txnHistAcctRepository
        .findByAccNoAndTranDateBetweenOrderByTranDateAscRcreTimeAsc(accountNo, fromDate, toDate);
    BigDecimal closingBalance = transactions.isEmpty() ? openingBalance : transactions.getLast().getBalanceAfterTran();
    return generateExcelSOA(accountDetails, openingBalance, closingBalance, transactions, fromDate, toDate);
}
```

### 2.8 Dashboard Metrics

The dashboard simply pulls counts using page size 1 to minimize payload and reads the `totalElements` metadata supplied by Spring pagination. No extra backend endpoints.

### 2.9 Admin: System Date & EOD (`/admin/system-date`, `/admin/eod`)

- **System Date page**
  - `GET /api/admin/eod/status` fetches `Parameter_Table.System_Date` through `SystemDateService`.
  - `POST /api/admin/set-system-date?systemDateStr=YYYY-MM-DD` updates that parameter and audits the change.
- **EOD page** orchestrates eight batch jobs through `EODJobController`:
  1. Account Balance Update → `Acct_Bal`
  2. Interest Accrual Transactions → `Intt_Accr_Tran`
  3. Interest Accrual GL Movements → `GL_Movement_Accrual`
  4. GL Movement Update → `GL_Movement`
  5. GL Balance Update → `GL_Balance`
  6. Interest Accrual Account Balances → `Acct_Bal_Accrual`
  7. Financial Reports → CSV/XLSX under `/reports/YYYYMMDD`
  8. System Date Increment → `Parameter_Table`

Job state is persisted in `EOD_Log_Table` and exposed via `GET /api/admin/eod/jobs/status`. Sequential execution is enforced server-side.

```23:128:/workspace/moneymarket/src/main/java/com/example/moneymarket/service/EODJobManagementService.java
public EODJobResult executeJob(int jobNumber, String userId) {
    LocalDate systemDate = systemDateService.getSystemDate();
    Optional<EODLogTable> existing = eodLogTableRepository
        .findTopByEodDateAndJobNameOrderByStartTimestampDesc(systemDate, getJobName(jobNumber));
    if (existing.isPresent() && existing.get().getStatus() == EODStatus.Success) {
        return EODJobResult.alreadyExecuted(jobNumber, jobName, existing.get().getStartTimestamp());
    }
    if (jobNumber > 1) {
        EODJobResult previousJob = checkPreviousJobCompletion(jobNumber - 1, systemDate);
        if (!previousJob.isSuccess()) {
            return EODJobResult.previousJobNotCompleted(jobNumber, jobName, previousJob.getMessage());
        }
    }
    EODLogTable logEntry = self.logJobStartInNewTransaction(systemDate, jobName, userId);
    try {
        int recordsProcessed = executeSpecificJob(jobNumber, systemDate);
        self.logJobSuccessInNewTransaction(logEntry, recordsProcessed);
        if (jobNumber == 8) {
            checkAndCompleteEODCycle(systemDate, userId);
        }
        return EODJobResult.success(jobNumber, jobName, recordsProcessed);
    } catch (Exception e) {
        self.logJobFailureInNewTransaction(logEntry, e.getMessage(), "Job execution");
        return EODJobResult.failure(jobNumber, jobName, e.getMessage());
    }
}
```

`SystemDateService` centralizes all non-device date usage; transactional code calls `getSystemDate()` rather than `LocalDate.now()` to keep books reproducible.

```38:104:/workspace/moneymarket/src/main/java/com/example/moneymarket/service/SystemDateService.java
public LocalDate getSystemDate() {
    Optional<String> systemDateStr = parameterTableRepository.getSystemDate();
    if (systemDateStr.isPresent() && !systemDateStr.get().trim().isEmpty()) {
        return LocalDate.parse(systemDateStr.get());
    }
    if (configuredSystemDate != null && !configuredSystemDate.trim().isEmpty()) {
        return LocalDate.parse(configuredSystemDate);
    }
    throw new SystemDateNotConfiguredException("System_Date is not configured.");
}

@Transactional
public void setSystemDate(LocalDate date, String userId) {
    parameterTableRepository.updateSystemDate(date.toString(), userId, LocalDateTime.now());
}
```

### 2.10 GL Setup Helpers

Dropdowns behind product/sub-product forms use `GLSetupService` to filter by chart-of-account layers and interest GL rules. GL validation occurs server-side via `GLNumberService` and `GLValidationService` before any account or sub-product is persisted.

## 3. Key Tables at a Glance

| Table | Purpose | Primary Columns Updated by UI |
| --- | --- | --- |
| `Cust_Master` | Customer registry with maker/checker audit | `Cust_Id`, `Ext_Cust_Id`, name fields, `Maker_Id`, `Verifier_Id`, `Verification_Date` |
| `Prod_Master` | Product catalog | `Product_Code`, `Product_Name`, `Cum_GL_Num`, flags, audit fields |
| `SubProd_Master` | Product variations (customer vs office) | `Sub_Product_Code`, `Cum_GL_Num`, interest GLs, `Sub_Product_Status` |
| `Cust_Acct_Master` | Customer accounts | `Account_No`, `Cust_Id`, `Sub_Product_Id`, `GL_Num`, tenor/maturity, `Account_Status`, `Loan_Limit` |
| `OFAcct_Master` | Office accounts | `Account_No`, `Sub_Product_Id`, `GL_Num`, `Acct_Name`, `Reconciliation_Required` |
| `Acct_Bal` | Daily account balances | `(Tran_Date, Account_No)` composite key, `Current_Balance`, `Available_Balance`, `Closing_Bal` |
| `Acct_Bal_Accrual` | Interest accrual balances | `Account_No`, `Tran_Date`, `Closing_Bal` |
| `Tran_Table` | Transaction legs | `Tran_Id`, `Tran_Status`, `Account_No`, `Dr_Cr_Flag`, `LCY_Amt` |
| `GL_Movement` / `GL_Movement_Accrual` | Ledger postings | `Movement_Id`, `GL_Num`, `Amount`, `Balance_After` |
| `GL_Balance` | Daily GL balances | `GL_Num`, `Tran_Date`, `Current_Balance` |
| `Txn_Hist_Acct` | Account statement history | `Acc_No`, `Tran_Id`, `Tran_Amt`, `Balance_After_Tran` |
| `Parameter_Table` | System-wide parameters (e.g., `System_Date`) | `Parameter_Value`, `Updated_By`, `Last_Updated` |
| `EOD_Log_Table` | Batch job audit | `EOD_Date`, `Job_Name`, `Status`, `Records_Processed`, `Error_Message` |

## 4. SQL Reference Material

- Full schema snapshot: `db backup/moneydb041125.sql`.
- Additional DDL/DML helpers: root-level `.sql` scripts (e.g., `add_gl_num_to_acct_bal_accrual.sql`, `critical_fixes_migration.sql`).
- Reports generated by Batch Job 7 land under `moneymarket/reports/YYYYMMDD/` as CSV/XLSX for Balance Sheet and Trial Balance verification.

## 5. Operational Considerations

- **System Date Discipline**: Always ensure `Parameter_Table.System_Date` is current before running batch jobs; front-end exposes manual override for test environments.
- **Maker–Checker**: Products, sub-products, customers, and transactions all require verification; editing resets verification so downstream approvals must repeat.
- **Account Number Collisions**: Customer numbers reserve the ninth digit for product category and last three digits for sequence. Monitor `AccountSeq` rows to avoid hitting the 999/99 caps.
- **Balance Integrity**: Posting or reversing transactions immediately updates both account- and GL-level balances; failures roll back inside a repeatable-read transaction.
- **Statements**: Only **verified** transactions populate `Txn_Hist_Acct`; unverified legs will not appear in SOA exports.
- **Error Surfacing**: Front-end normalizes 4xx/5xx errors and enforces redirects to `/login` on 401 responses, so backend should return meaningful `ErrorResponseDTO` messages.

Use this guide alongside the service-layer code snippets to trace data mutations end-to-end when investigating production issues or planning new features.

