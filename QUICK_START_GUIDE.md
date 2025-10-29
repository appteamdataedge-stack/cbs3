# Quick Start Guide - EOD Batch Jobs Implementation

## Pre-Deployment Steps

### 1. Database Migration
```bash
# Connect to MySQL database
mysql -u your_username -p moneymarketdb

# Run the migration script
source database_migration_eod_batch_jobs.sql
```

### 2. Verify Database Changes
```sql
-- Check new columns in Sub_Prod_Master
DESCRIBE Sub_Prod_Master;

-- Verify indices created
SHOW INDEX FROM Intt_Accr_Tran;
SHOW INDEX FROM GL_Movement;
SHOW INDEX FROM GL_Balance;
```

### 3. Configure Sub-Products

Update your sub-products with GL account mappings:

```sql
-- Example: Savings Account (Liability)
UPDATE Sub_Prod_Master
SET Interest_Payable_GL_Num = '120101001',      -- Interest Payable GL
    Interest_Expenditure_GL_Num = '140101001',  -- Interest Expense GL
    Interest_Income_GL_Num = NULL,
    Interest_Receivable_GL_Num = NULL,
    Interest_Increment = 0.0000,                -- Additional rate on top of base
    Effective_Interest_Rate = 5.5000            -- Fixed rate for deal accounts
WHERE Sub_Product_Code = 'SAV001';

-- Example: Loan Account (Asset)
UPDATE Sub_Prod_Master
SET Interest_Payable_GL_Num = NULL,
    Interest_Expenditure_GL_Num = NULL,
    Interest_Income_GL_Num = '140201001',       -- Interest Income GL
    Interest_Receivable_GL_Num = '210201001',   -- Interest Receivable GL
    Interest_Increment = 1.0000,                -- Additional 1% on top of base rate
    Effective_Interest_Rate = NULL
WHERE Sub_Product_Code = 'LOAN001';
```

### 4. Create Office Accounts

Ensure office accounts exist for all interest GL numbers:

```sql
-- Example: Create office account for Interest Payable
INSERT INTO OF_Acct_Master (
    Account_No, Sub_Product_Id, GL_Num, Acct_Name,
    Date_Opening, Branch_Code, Account_Status, Reconciliation_Required
) VALUES (
    '1201010010001',           -- Office account number
    1,                         -- Sub_Product_Id for office accounts
    '120101001',               -- GL_Num matching Interest_Payable_GL_Num
    'Interest Payable Control',
    CURRENT_DATE,
    'HEAD',
    'Active',
    TRUE
);
```

### 5. Configure Interest Rate Master

Add base interest rates with effective dates:

```sql
INSERT INTO Interest_Rate_Master (Intt_Code, Intt_Rate, Intt_Effctv_Date)
VALUES
    ('SAVRATE', 4.50, '2025-01-01'),  -- Savings base rate
    ('LOANRATE', 10.00, '2025-01-01'); -- Loan base rate
```

### 6. Application Configuration

Update `application.properties`:

```properties
# Reports directory
reports.directory=reports

# Currency configuration
currency.default=BDT

# Interest calculation
interest.default.divisor=36500

# EOD configuration
eod.admin.user=ADMIN
```

## Running EOD Process

### Via REST API

```bash
# Trigger EOD process
curl -X POST http://localhost:8080/api/eod/execute \
  -H "Content-Type: application/json" \
  -d '{"userId": "ADMIN"}'
```

### Check EOD Status

```sql
-- View EOD execution logs
SELECT * FROM EOD_Log_Table
WHERE EOD_Date = CURRENT_DATE
ORDER BY Start_Timestamp DESC;

-- Check for failed jobs
SELECT * FROM EOD_Log_Table
WHERE Status = 'Failed'
ORDER BY Start_Timestamp DESC
LIMIT 10;
```

## Verification Steps

### 1. Verify Interest Accruals (Batch Job 2)

```sql
-- Check interest accrual transactions created
SELECT
    Tran_Id,
    Account_No,
    Dr_Cr_Flag,
    Amount,
    GL_Account_No,
    Narration
FROM Intt_Accr_Tran
WHERE Accrual_Date = (SELECT Parameter_Value FROM Parameter_Table WHERE Parameter_Name = 'System_Date')
ORDER BY Tran_Id, Dr_Cr_Flag;

-- Verify two entries per account (one Dr, one Cr)
SELECT
    Tran_Id,
    COUNT(*) as entry_count,
    SUM(CASE WHEN Dr_Cr_Flag = 'D' THEN 1 ELSE 0 END) as debit_count,
    SUM(CASE WHEN Dr_Cr_Flag = 'C' THEN 1 ELSE 0 END) as credit_count,
    SUM(CASE WHEN Dr_Cr_Flag = 'D' THEN Amount ELSE -Amount END) as net_amount
FROM Intt_Accr_Tran
WHERE Accrual_Date = (SELECT Parameter_Value FROM Parameter_Table WHERE Parameter_Name = 'System_Date')
GROUP BY Tran_Id
HAVING net_amount != 0;  -- Should return no rows (balanced)
```

### 2. Verify GL Movements (Batch Job 3 & 4)

```sql
-- Check GL movements from accruals
SELECT COUNT(*) as accrual_movements
FROM GL_Movement_Accrual
WHERE Accrual_Date = (SELECT Parameter_Value FROM Parameter_Table WHERE Parameter_Name = 'System_Date');

-- Check GL movements from transactions
SELECT COUNT(*) as transaction_movements
FROM GL_Movement
WHERE Tran_Date = (SELECT Parameter_Value FROM Parameter_Table WHERE Parameter_Name = 'System_Date');
```

### 3. Verify GL Balances (Batch Job 5)

```sql
-- Check GL balances are balanced (sum should be 0)
SELECT
    SUM(Closing_Bal) as total_closing_balance,
    COUNT(*) as gl_accounts_processed
FROM GL_Balance
WHERE Tran_Date = (SELECT Parameter_Value FROM Parameter_Table WHERE Parameter_Name = 'System_Date');

-- Should show total_closing_balance = 0.00

-- Breakdown by category
SELECT
    CASE
        WHEN GL_Num LIKE '1%' THEN 'Liabilities'
        WHEN GL_Num LIKE '2%' THEN 'Assets'
        ELSE 'Other'
    END as category,
    COUNT(*) as gl_count,
    SUM(Closing_Bal) as total_balance
FROM GL_Balance
WHERE Tran_Date = (SELECT Parameter_Value FROM Parameter_Table WHERE Parameter_Name = 'System_Date')
GROUP BY category;
```

### 4. Verify Reports Generated (Batch Job 7)

```bash
# Check if report files exist
ls -l reports/$(date +%Y%m%d)/

# Should show:
# TrialBalance_YYYYMMDD.csv
# BalanceSheet_YYYYMMDD.csv

# View Trial Balance
cat reports/$(date +%Y%m%d)/TrialBalance_*.csv | head -20

# View Balance Sheet
cat reports/$(date +%Y%m%d)/BalanceSheet_*.csv | head -30
```

## Troubleshooting

### Issue: Books Not Balanced

```sql
-- Find imbalanced GLs
SELECT
    GB.GL_Num,
    GS.GL_Name,
    GB.Opening_Bal,
    GB.DR_Summation,
    GB.CR_Summation,
    GB.Closing_Bal
FROM GL_Balance GB
JOIN GL_Setup GS ON GB.GL_Num = GS.GL_Num
WHERE GB.Tran_Date = (SELECT Parameter_Value FROM Parameter_Table WHERE Parameter_Name = 'System_Date')
ORDER BY ABS(GB.Closing_Bal) DESC;

-- Check for unmatched transactions
SELECT
    T.Tran_Id,
    T.Account_No,
    T.Dr_Cr_Flag,
    T.LCY_Amt,
    CASE WHEN GM.Movement_Id IS NULL THEN 'Missing GL Movement' ELSE 'OK' END as status
FROM Tran_Table T
LEFT JOIN GL_Movement GM ON T.Tran_Id = GM.Tran_Id
WHERE T.Tran_Date = (SELECT Parameter_Value FROM Parameter_Table WHERE Parameter_Name = 'System_Date')
  AND T.Tran_Status = 'Verified';
```

### Issue: Interest Accruals Not Created

```sql
-- Check active accounts without accruals
SELECT
    CA.Account_No,
    CA.Acct_Name,
    SP.Sub_Product_Code,
    SP.Intt_Code,
    AB.Closing_Bal
FROM Cust_Acct_Master CA
JOIN Sub_Prod_Master SP ON CA.Sub_Product_Id = SP.Sub_Product_Id
JOIN Acct_Bal AB ON CA.Account_No = AB.Account_No
LEFT JOIN Intt_Accr_Tran IAT ON CA.Account_No = IAT.Account_No
    AND IAT.Accrual_Date = (SELECT Parameter_Value FROM Parameter_Table WHERE Parameter_Name = 'System_Date')
WHERE CA.Account_Status = 'Active'
  AND AB.Closing_Bal > 0
  AND IAT.Accr_Id IS NULL;

-- Check interest rate configuration
SELECT * FROM Interest_Rate_Master
WHERE Intt_Code IN (SELECT DISTINCT Intt_Code FROM Sub_Prod_Master WHERE Intt_Code IS NOT NULL)
ORDER BY Intt_Code, Intt_Effctv_Date DESC;
```

### Issue: Missing Office Accounts

```sql
-- Find GL numbers without office accounts
SELECT DISTINCT
    COALESCE(SP.Interest_Payable_GL_Num, SP.Interest_Income_GL_Num) as GL_Num,
    CASE
        WHEN SP.Interest_Payable_GL_Num IS NOT NULL THEN 'Interest Payable'
        ELSE 'Interest Income'
    END as GL_Type
FROM Sub_Prod_Master SP
LEFT JOIN OF_Acct_Master OA ON COALESCE(SP.Interest_Payable_GL_Num, SP.Interest_Income_GL_Num) = OA.GL_Num
WHERE COALESCE(SP.Interest_Payable_GL_Num, SP.Interest_Income_GL_Num) IS NOT NULL
  AND OA.Account_No IS NULL;
```

### Issue: Report Validation Failed

Check application logs for detailed error messages:

```bash
# View recent logs
tail -f logs/moneymarket.log | grep "Batch Job 7"

# Check for validation errors
grep "validation failed" logs/moneymarket.log
```

## Common Operations

### Rerun a Failed Batch Job

```bash
# Rerun specific batch job (example: Batch Job 5)
curl -X POST http://localhost:8080/api/eod/batch/5 \
  -H "Content-Type: application/json" \
  -d '{"userId": "ADMIN"}'
```

### Manual System Date Increment

```sql
UPDATE Parameter_Table
SET Parameter_Value = DATE_ADD(STR_TO_DATE(Parameter_Value, '%Y-%m-%d'), INTERVAL 1 DAY),
    Updated_By = 'ADMIN',
    Last_Updated = NOW()
WHERE Parameter_Name = 'System_Date';
```

### Clean Up Test Data

```sql
-- Delete EOD data for a specific date (use with caution!)
SET @test_date = '2025-01-15';

DELETE FROM Intt_Accr_Tran WHERE Accrual_Date = @test_date;
DELETE FROM GL_Movement_Accrual WHERE Accrual_Date = @test_date;
DELETE FROM GL_Movement WHERE Tran_Date = @test_date;
DELETE FROM GL_Balance WHERE Tran_Date = @test_date;
DELETE FROM Acct_Bal_Accrual WHERE Tran_Date = @test_date;
DELETE FROM EOD_Log_Table WHERE EOD_Date = @test_date;
```

## Performance Monitoring

```sql
-- Check batch job execution times
SELECT
    Job_Name,
    Start_Timestamp,
    End_Timestamp,
    TIMESTAMPDIFF(SECOND, Start_Timestamp, End_Timestamp) as duration_seconds,
    Records_Processed,
    Status
FROM EOD_Log_Table
WHERE EOD_Date = CURRENT_DATE
ORDER BY Start_Timestamp;

-- Average execution times by job
SELECT
    Job_Name,
    AVG(TIMESTAMPDIFF(SECOND, Start_Timestamp, End_Timestamp)) as avg_duration_seconds,
    MIN(TIMESTAMPDIFF(SECOND, Start_Timestamp, End_Timestamp)) as min_duration_seconds,
    MAX(TIMESTAMPDIFF(SECOND, Start_Timestamp, End_Timestamp)) as max_duration_seconds,
    COUNT(*) as execution_count
FROM EOD_Log_Table
WHERE Status = 'Success'
GROUP BY Job_Name
ORDER BY avg_duration_seconds DESC;
```

## Support

For detailed implementation information, see:
- `EOD_IMPLEMENTATION_SUMMARY.md` - Comprehensive implementation details
- `database_migration_eod_batch_jobs.sql` - Database migration script
- Application logs in `logs/moneymarket.log`

For issues:
1. Check EOD_Log_Table for error messages
2. Review application logs for stack traces
3. Verify all prerequisites are configured
4. Run verification queries above
5. Contact development team with specific error details
