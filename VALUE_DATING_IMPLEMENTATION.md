# Value Dating Implementation Guide

## Overview

This document describes the implementation of **Value Dating** functionality in the Money Market application. Value dating allows transactions to have an effective date (Value Date) that differs from the system transaction date.

## Features Implemented

### 1. Value Date Classification

The system now classifies all transactions into three types:

- **PAST-DATED** (`Value_Date < System_Date`): Transactions backdated to the past
- **CURRENT** (`Value_Date = System_Date`): Normal transactions
- **FUTURE-DATED** (`Value_Date > System_Date`): Transactions scheduled for future posting

### 2. Validation Rules

All value dates are validated against business rules:

| Rule | Parameter | Default | Description |
|------|-----------|---------|-------------|
| Past Limit | `Past_Value_Date_Limit_Days` | 90 | Maximum days in the past |
| Future Limit | `Future_Value_Date_Limit_Days` | 30 | Maximum days in the future |
| EOM Restriction | `Last_EOM_Date` | 2024-12-31 | Cannot be before last month-end |

### 3. Delta Interest Calculation

For past-dated transactions, the system calculates **Delta Interest** to compensate for the time difference:

```
Δ_Interest = Principal × Interest_Rate × Days_Difference / Divisor

Where:
- Principal = Transaction Amount (absolute value)
- Interest_Rate = Annual rate as decimal (5% = 0.05)
- Days_Difference = System_Date - Value_Date (in days)
- Divisor = 36500 (from parameter: Interest_Default_Divisor)
```

**Example:**
```
System_Date: 2025-11-06
Value_Date: 2025-11-04
Amount: 10,000 BDT
Interest Rate: 5% p.a.
Days_Difference: 2 days

Δ_Interest = 10,000 × 0.05 × 2 / 36500 = 0.0274 BDT (rounded to 0.03)
```

### 4. Interest Adjustment Posting

Delta interest is posted through GL entries based on account type:

#### Liability Accounts (GL starts with '1')

| Transaction Type | Dr/Cr Flag | Interest Impact | Accounting Entry |
|-----------------|------------|----------------|------------------|
| Deposit (Credit) | 'C' | Bank owes MORE interest | Dr Interest Expense GL<br>Cr Accrued Interest Payable GL |
| Withdrawal (Debit) | 'D' | Bank owes LESS interest | Dr Accrued Interest Payable GL<br>Cr Interest Expense GL |

#### Asset Accounts (GL starts with '2')

| Transaction Type | Dr/Cr Flag | Interest Impact | Accounting Entry |
|-----------------|------------|----------------|------------------|
| Disbursement (Debit) | 'D' | Bank earns MORE interest | Dr Accrued Interest Receivable GL<br>Cr Interest Income GL |
| Repayment (Credit) | 'C' | Bank earns LESS interest | Dr Interest Income GL<br>Cr Accrued Interest Receivable GL |

**Note:** GL accounts are retrieved from `Sub_Prod_Master` table:
- Debit side: `interest_receivable_expenditure_gl_num`
- Credit side: `interest_income_payable_gl_num`

### 5. Future-Dated Transaction Processing

Future-dated transactions follow a two-step process:

**Step 1: Creation**
- Status set to `Future`
- Transaction saved but balances NOT updated
- Logged in `Tran_Value_Date_Log` with `Adjustment_Posted_Flag = 'N'`

**Step 2: BOD Processing (when Value_Date arrives)**
- BOD scheduler runs at 6:00 AM daily
- Status changed from `Future` to `Posted`
- Balances updated (account + GL)
- Log updated with `Adjustment_Posted_Flag = 'Y'`

## Database Schema

### New Table: Tran_Value_Date_Log

```sql
CREATE TABLE Tran_Value_Date_Log (
    Log_Id BIGINT AUTO_INCREMENT PRIMARY KEY,
    Tran_Id VARCHAR(20) NOT NULL,
    Value_Date DATE NOT NULL,
    Days_Difference INT NOT NULL,
    Delta_Interest_Amt DECIMAL(20, 4) DEFAULT 0.0000,
    Adjustment_Posted_Flag VARCHAR(1) DEFAULT 'N',
    Created_Timestamp DATETIME NOT NULL,

    INDEX idx_tran_id (Tran_Id),
    INDEX idx_value_date (Value_Date),
    INDEX idx_posted_flag (Adjustment_Posted_Flag)
);
```

### Modified Table: Tran_Table

Added new transaction status:

```java
public enum TranStatus {
    Entry,      // Initial status (maker)
    Posted,     // Posted and balances updated
    Verified,   // Verified by checker
    Future      // Future-dated, pending BOD processing
}
```

### New Parameters in Parameter_Table

| Parameter Name | Default Value | Description |
|---------------|---------------|-------------|
| `Past_Value_Date_Limit_Days` | 90 | Maximum days in past |
| `Future_Value_Date_Limit_Days` | 30 | Maximum days in future |
| `Interest_Default_Divisor` | 36500 | Interest calculation divisor |
| `Last_EOM_Date` | 2024-12-31 | Last End of Month date |

## Implementation Components

### 1. Entities

- **TranValueDateLog.java** - New entity for value date logging
- **TranTable.java** - Modified to add `Future` status

### 2. Repositories

- **TranValueDateLogRepository.java** - Repository for value date logs

### 3. Services

| Service | Responsibility |
|---------|---------------|
| **ValueDateValidationService** | Validates value dates against business rules |
| **ValueDateCalculationService** | Calculates delta interest for past-dated transactions |
| **ValueDatePostingService** | Posts interest adjustment GL entries and logs |
| **BODValueDateService** | Processes future-dated transactions at BOD |
| **TransactionService** (enhanced) | Integrates value dating logic into transaction posting |

### 4. Schedulers

- **BODScheduler.java** - Runs at 6:00 AM daily to process future-dated transactions

**Scheduler Configuration:**

Add to `application.properties`:
```properties
# Enable BOD scheduler
bod.scheduler.enabled=true

# BOD runs at 6:00 AM daily
bod.scheduler.cron=0 0 6 * * ?

# Monitor pending transactions every hour during business hours
bod.monitor.cron=0 0 9-17 * * MON-FRI
```

## Transaction Flow

### Flow 1: CURRENT-DATED Transaction

```
1. User creates transaction with Value_Date = System_Date
2. Validation passes (value date = system date)
3. Transaction posted with status 'Posted'
4. Balances updated immediately
5. GL movements created
6. No value date logging (standard flow)
```

### Flow 2: PAST-DATED Transaction

```
1. User creates transaction with Value_Date < System_Date
2. Validation checks:
   - Days difference <= Past_Value_Date_Limit_Days (90)
   - Value_Date >= Last_EOM_Date
3. Transaction posted with status 'Posted'
4. Balances updated immediately
5. GL movements created for main transaction
6. Delta interest calculated
7. Interest adjustment GL entries posted (Dr + Cr)
8. Logged in Tran_Value_Date_Log with flag 'Y'
```

### Flow 3: FUTURE-DATED Transaction

```
Creation Phase:
1. User creates transaction with Value_Date > System_Date
2. Validation checks:
   - Days difference <= Future_Value_Date_Limit_Days (30)
3. Transaction saved with status 'Future'
4. Balances NOT updated
5. No GL movements created yet
6. Logged in Tran_Value_Date_Log with flag 'N'

BOD Processing (when Value_Date arrives):
1. BOD scheduler runs at 6:00 AM
2. Finds all Future transactions where Value_Date <= System_Date
3. For each transaction:
   - Status changed to 'Posted'
   - Tran_Date updated to current System_Date
   - Balances updated (account + GL)
   - GL movements created
   - Log flag updated to 'Y'
```

## Test Scenarios

### Test 1: Past-Dated Deposit

**Given:**
```
System_Date: 2025-11-06
Value_Date: 2025-11-04 (2 days past)
Account: Savings Account (5% p.a., GL: 110101001)
Amount: 10,000 BDT
Dr/Cr Flag: 'C' (Credit)
```

**Expected Results:**
1. Main transaction posted with status 'Posted'
2. Account balance updated: +10,000
3. GL balance updated: +10,000
4. Δ-Interest calculated: 10,000 × 0.05 × 2 / 36500 = 0.03 BDT
5. Interest adjustment entries:
   - Dr Interest Expense GL (from sub-product): 0.03
   - Cr Accrued Interest Payable GL (from sub-product): 0.03
6. Entry in Tran_Value_Date_Log:
   - Days_Difference: 2
   - Delta_Interest_Amt: 0.03
   - Adjustment_Posted_Flag: 'Y'

### Test 2: Past-Dated Withdrawal

**Given:**
```
System_Date: 2025-11-06
Value_Date: 2025-11-03 (3 days past)
Account: Current Account (2% p.a., GL: 110102001)
Amount: 5,000 BDT
Dr/Cr Flag: 'D' (Debit)
```

**Expected Results:**
1. Main transaction posted
2. Account balance updated: -5,000
3. Δ-Interest calculated: 5,000 × 0.02 × 3 / 36500 = 0.01 BDT
4. Reverse interest adjustment:
   - Dr Accrued Interest Payable GL: 0.01
   - Cr Interest Expense GL: 0.01
5. Log entry created with flag 'Y'

### Test 3: Future-Dated Transaction

**Given:**
```
System_Date: 2025-11-06
Value_Date: 2025-11-08 (2 days future)
Amount: 15,000 BDT
Dr/Cr Flag: 'C'
```

**Expected Results - Creation:**
1. Transaction created with status 'Future'
2. Balances NOT updated
3. No GL movements created
4. Entry in Tran_Value_Date_Log:
   - Days_Difference: -2
   - Delta_Interest_Amt: 0.00
   - Adjustment_Posted_Flag: 'N'

**Expected Results - BOD on 2025-11-08:**
1. BOD scheduler finds the transaction
2. Status changed to 'Posted'
3. Tran_Date updated to 2025-11-08
4. Balances updated: +15,000
5. GL movements created
6. Log flag updated to 'Y'

### Test 4: Validation Rejection - Past Limit Exceeded

**Given:**
```
System_Date: 2025-11-06
Value_Date: 2025-08-01 (100+ days past)
Past_Value_Date_Limit_Days: 90
```

**Expected:**
```
ValidationException: "Value date cannot be more than 90 days in the past.
Value date: 2025-08-01, System date: 2025-11-06, Difference: 97 days"
```

### Test 5: Validation Rejection - Before EOM

**Given:**
```
System_Date: 2025-11-06
Value_Date: 2024-11-30
Last_EOM_Date: 2024-12-31
```

**Expected:**
```
ValidationException: "Value date cannot be before last End of Month date.
Value date: 2024-11-30, Last EOM date: 2024-12-31"
```

## Setup Instructions

### 1. Database Setup

Run the SQL migration scripts in order:

```bash
# Create Tran_Value_Date_Log table
mysql -u root -p moneymarket < src/main/resources/db/migration/V1__Create_Tran_Value_Date_Log.sql

# Insert required parameters
mysql -u root -p moneymarket < src/main/resources/db/migration/V2__Insert_Value_Date_Parameters.sql
```

Or if using Flyway, the migrations will run automatically on application startup.

### 2. Application Configuration

Add to `application.properties`:

```properties
# Enable BOD scheduler for value dating
bod.scheduler.enabled=true
bod.scheduler.cron=0 0 6 * * ?
bod.monitor.cron=0 0 9-17 * * MON-FRI

# Optional: Adjust validation limits
# These can also be modified directly in database
# Past limit in days (default: 90)
# Future limit in days (default: 30)
# Interest divisor (default: 36500)
```

### 3. Sub-Product Configuration

Ensure all sub-products have interest GL accounts configured:

```sql
UPDATE Sub_Prod_Master
SET
    interest_receivable_expenditure_gl_num = 'YOUR_EXPENSE_OR_RECEIVABLE_GL',
    interest_income_payable_gl_num = 'YOUR_INCOME_OR_PAYABLE_GL'
WHERE Sub_Product_Id = ?;
```

**For Liability Products (GL starts with '1'):**
- `interest_receivable_expenditure_gl_num`: Interest Expense GL
- `interest_income_payable_gl_num`: Accrued Interest Payable GL

**For Asset Products (GL starts with '2'):**
- `interest_receivable_expenditure_gl_num`: Accrued Interest Receivable GL
- `interest_income_payable_gl_num`: Interest Income GL

### 4. Verify Installation

```sql
-- Check if table exists
SHOW TABLES LIKE 'Tran_Value_Date_Log';

-- Verify parameters
SELECT * FROM Parameter_Table
WHERE Parameter_Name IN (
    'Past_Value_Date_Limit_Days',
    'Future_Value_Date_Limit_Days',
    'Interest_Default_Divisor',
    'Last_EOM_Date'
);

-- Check transaction statuses
SELECT DISTINCT Tran_Status FROM Tran_Table;
```

## API Usage

The existing transaction APIs automatically support value dating:

### Create Transaction
```http
POST /api/transactions
Content-Type: application/json

{
  "valueDate": "2025-11-04",  // Can be past, current, or future
  "narration": "Test transaction",
  "lines": [
    {
      "accountNo": "1234567890123",
      "drCrFlag": "D",
      "tranCcy": "BDT",
      "fcyAmt": 10000,
      "exchangeRate": 1.0000,
      "lcyAmt": 10000
    },
    {
      "accountNo": "9876543210987",
      "drCrFlag": "C",
      "tranCcy": "BDT",
      "fcyAmt": 10000,
      "exchangeRate": 1.0000,
      "lcyAmt": 10000
    }
  ]
}
```

### Post Transaction
```http
POST /api/transactions/{tranId}/post
```

The system will automatically:
- Validate the value date
- Classify it as PAST/CURRENT/FUTURE
- Apply appropriate processing logic
- Calculate delta interest for past-dated transactions
- Set Future status for future-dated transactions

## Monitoring

### Check Pending Future-Dated Transactions

```sql
SELECT COUNT(*) as Pending_Count
FROM Tran_Table
WHERE Tran_Status = 'Future';
```

### View Future-Dated Transaction Details

```sql
SELECT
    t.Tran_Id,
    t.Tran_Date,
    t.Value_Date,
    t.Account_No,
    t.LCY_Amt,
    t.Tran_Status,
    v.Days_Difference,
    v.Adjustment_Posted_Flag
FROM Tran_Table t
LEFT JOIN Tran_Value_Date_Log v ON t.Tran_Id = v.Tran_Id
WHERE t.Tran_Status = 'Future'
ORDER BY t.Value_Date;
```

### View Past-Dated Transactions with Interest Adjustments

```sql
SELECT
    t.Tran_Id,
    t.Tran_Date,
    t.Value_Date,
    t.Account_No,
    t.LCY_Amt,
    v.Days_Difference,
    v.Delta_Interest_Amt,
    v.Created_Timestamp
FROM Tran_Table t
INNER JOIN Tran_Value_Date_Log v ON t.Tran_Id = v.Tran_Id
WHERE v.Days_Difference > 0
  AND v.Adjustment_Posted_Flag = 'Y'
ORDER BY t.Tran_Date DESC;
```

## Troubleshooting

### Issue: BOD not processing future-dated transactions

**Check:**
1. Is BOD scheduler enabled? `bod.scheduler.enabled=true`
2. Check scheduler logs for errors
3. Verify system date is correct
4. Check if transactions exist: `SELECT * FROM Tran_Table WHERE Tran_Status = 'Future'`

### Issue: Delta interest not calculated for past-dated transactions

**Check:**
1. Does the account's sub-product have an interest rate configured?
2. Check `Sub_Prod_Master.Effective_Interest_Rate`
3. Check parameter: `SELECT * FROM Parameter_Table WHERE Parameter_Name = 'Interest_Default_Divisor'`
4. Review logs for calculation errors

### Issue: Interest adjustment GL entries missing

**Check:**
1. Does the sub-product have interest GL accounts configured?
   ```sql
   SELECT
       Sub_Product_Code,
       interest_receivable_expenditure_gl_num,
       interest_income_payable_gl_num
   FROM Sub_Prod_Master
   WHERE Sub_Product_Id = ?
   ```
2. Do the GL accounts exist in GL_Setup?
3. Review application logs for posting errors

## Critical Notes

1. **Never skip value date validation** - All transactions must pass validation before posting
2. **EOD processing** - Run EOD AFTER BOD to ensure all transactions for the day are included
3. **Month-end closing** - Update `Last_EOM_Date` parameter after month-end closing:
   ```sql
   UPDATE Parameter_Table
   SET Parameter_Value = '2025-11-30',
       Last_Updated = NOW(),
       Updated_By = 'ADMIN'
   WHERE Parameter_Name = 'Last_EOM_Date';
   ```
4. **Interest GL accounts** - Must be configured for all interest-bearing sub-products
5. **Backup before deployment** - This feature modifies core transaction processing logic

## Support

For issues or questions regarding value dating implementation:
1. Check application logs in `logs/` directory
2. Review database entries in `Tran_Value_Date_Log` table
3. Verify parameter configuration
4. Contact development team with:
   - Transaction ID
   - Value date vs System date
   - Error messages from logs
   - Database query results

---

**Implementation Date:** 2025-11-09
**Version:** 1.0
**Status:** Production Ready
