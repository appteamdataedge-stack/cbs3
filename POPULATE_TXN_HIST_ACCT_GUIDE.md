# Guide: Create and Populate TXN_HIST_ACCT Table

## 🎯 What This Does

This script will:
1. ✅ Create the `txn_hist_acct` table
2. ✅ Populate it with ALL existing verified transactions from `tran_table`
3. ✅ Calculate accurate opening and closing balances for each transaction
4. ✅ Process transactions in chronological order
5. ✅ Handle both customer and office accounts
6. ✅ Assign proper serial numbers for multi-leg transactions

## 🚀 Quick Start (Easiest Method)

### Step 1: Double-Click the Batch File
1. Find `create_and_populate_soa_table.bat` in your project root
2. **Double-click it**
3. Press any key when prompted
4. Enter your MySQL root password
5. Wait for completion (may take 1-5 minutes depending on data volume)

### Step 2: Verify Success
You should see:
```
========================================
SUCCESS!
========================================

Table created and populated successfully!

You can now use the Statement of Accounts module.
```

## 📊 What the Script Does

### Phase 1: Table Creation
Creates the `txn_hist_acct` table with:
- 18 columns for transaction details
- 5 indexes for optimal performance
- Foreign key to `tran_table`

### Phase 2: Data Migration
Processes all verified transactions:
1. **Fetches** all transactions with status = 'Verified'
2. **Orders** them by date and transaction ID
3. **Calculates** opening balance for each transaction
4. **Computes** closing balance (opening ± transaction amount)
5. **Inserts** record into `txn_hist_acct`
6. **Updates** running balance for next transaction

### Phase 3: Balance Calculation Logic

For each transaction:
```
Opening Balance = Previous transaction's closing balance
                  OR acct_bal.Current_Balance
                  OR 0 (if no previous data)

If CREDIT (C):
    Closing Balance = Opening Balance + Transaction Amount

If DEBIT (D):
    Closing Balance = Opening Balance - Transaction Amount
```

### Phase 4: Verification
Shows summary statistics:
- Total records inserted
- Count by transaction type (Debit/Credit)
- Sample of recent records
- Top accounts by transaction count

## 📋 Alternative Methods

### Method 1: MySQL Command Line
```bash
cd "G:\Backup-Source Code\Money Market PTTP-reback"
mysql -u root -p moneymarketdb < create_and_populate_txn_hist_acct.sql
```

### Method 2: MySQL Workbench
1. Open MySQL Workbench
2. Connect to `moneymarketdb`
3. File → Open SQL Script
4. Select `create_and_populate_txn_hist_acct.sql`
5. Click Execute (⚡ icon)
6. Wait for completion

### Method 3: Copy-Paste into MySQL
1. Open the SQL file in a text editor
2. Copy all contents
3. Connect to MySQL: `mysql -u root -p moneymarketdb`
4. Paste and press Enter
5. Wait for completion

## 🔍 Verification Queries

After running the script, verify the data:

### Check Total Records
```sql
SELECT COUNT(*) AS 'Total Records' FROM txn_hist_acct;
```

### Check Sample Data
```sql
SELECT 
    Hist_ID,
    ACC_No,
    TRAN_ID,
    TRAN_DATE,
    TRAN_TYPE,
    TRAN_AMT,
    Opening_Balance,
    BALANCE_AFTER_TRAN
FROM txn_hist_acct
ORDER BY TRAN_DATE DESC
LIMIT 10;
```

### Check Balance Accuracy
```sql
-- Compare with acct_bal table
SELECT 
    h.ACC_No,
    h.BALANCE_AFTER_TRAN AS 'History Balance',
    ab.Current_Balance AS 'AcctBal Balance',
    (h.BALANCE_AFTER_TRAN - ab.Current_Balance) AS 'Difference'
FROM (
    SELECT ACC_No, BALANCE_AFTER_TRAN
    FROM txn_hist_acct
    WHERE (ACC_No, TRAN_DATE, RCRE_TIME) IN (
        SELECT ACC_No, MAX(TRAN_DATE), MAX(RCRE_TIME)
        FROM txn_hist_acct
        GROUP BY ACC_No
    )
) h
LEFT JOIN acct_bal ab ON h.ACC_No = ab.Account_No
LIMIT 10;
```

### Check Transactions by Account
```sql
SELECT 
    ACC_No,
    COUNT(*) AS 'Transaction Count',
    SUM(CASE WHEN TRAN_TYPE = 'D' THEN TRAN_AMT ELSE 0 END) AS 'Total Debits',
    SUM(CASE WHEN TRAN_TYPE = 'C' THEN TRAN_AMT ELSE 0 END) AS 'Total Credits',
    MIN(TRAN_DATE) AS 'First Transaction',
    MAX(TRAN_DATE) AS 'Last Transaction'
FROM txn_hist_acct
GROUP BY ACC_No
ORDER BY COUNT(*) DESC
LIMIT 20;
```

## ⏱️ Expected Duration

| Number of Transactions | Expected Time |
|------------------------|---------------|
| < 1,000                | 10-30 seconds |
| 1,000 - 10,000         | 30-60 seconds |
| 10,000 - 100,000       | 1-3 minutes   |
| > 100,000              | 3-10 minutes  |

## ✅ Success Indicators

You'll know it worked when:

1. ✅ Script completes without errors
2. ✅ Table `txn_hist_acct` exists
3. ✅ Records count matches verified transactions in `tran_table`
4. ✅ Balances look reasonable (no negative balances for deposit accounts)
5. ✅ Opening and closing balances are sequential
6. ✅ SOA generation works in the UI

## 🐛 Troubleshooting

### Issue: "Table doesn't exist" error during population
**Cause:** Foreign key constraint fails if `tran_table` doesn't exist  
**Solution:** 
1. Verify `tran_table` exists: `SHOW TABLES LIKE 'tran_table';`
2. If not, you need to create it first
3. Or modify the script to remove the foreign key constraint

### Issue: Script takes too long
**Cause:** Large number of transactions  
**Solution:** 
1. This is normal for large datasets
2. Let it complete (may take 5-10 minutes)
3. Don't interrupt the process

### Issue: "Duplicate entry" error
**Cause:** Table already exists with data  
**Solution:** 
1. The script drops the table first, so this shouldn't happen
2. If it does, manually drop: `DROP TABLE txn_hist_acct;`
3. Run the script again

### Issue: Balances seem incorrect
**Cause:** Missing data in `acct_bal` table or incorrect transaction order  
**Solution:** 
1. Check if `acct_bal` has accurate data
2. Verify transactions in `tran_table` are in correct order
3. The script processes in chronological order, so if source data is wrong, results will be wrong

### Issue: Some accounts missing
**Cause:** Transactions not in 'Verified' status  
**Solution:** 
1. Only verified transactions are migrated
2. Check transaction status: `SELECT Tran_Status, COUNT(*) FROM tran_table GROUP BY Tran_Status;`
3. If needed, verify pending transactions first

## 🔄 Re-running the Script

If you need to re-run the script:

1. **Safe to re-run:** The script drops the table first
2. **Data loss:** All existing `txn_hist_acct` data will be deleted
3. **Fresh start:** Recalculates all balances from scratch

To re-run:
```bash
# Just double-click the batch file again
# OR
mysql -u root -p moneymarketdb < create_and_populate_txn_hist_acct.sql
```

## 📊 Understanding the Output

The script shows several verification results:

### 1. Total Records Inserted
```
Total Records Inserted: 1,234
```
This should match (or be close to) the number of verified transactions in `tran_table`.

### 2. Count by Transaction Type
```
TRAN_TYPE | Count | Total Amount
D         | 567   | 1,234,567.89
C         | 667   | 1,345,678.90
```
Shows breakdown of debits vs credits.

### 3. Sample Records
Shows the 10 most recent transactions with balances.

### 4. Top Accounts
Shows which accounts have the most transactions.

## 🎯 After Population

### What Happens Next?

1. **Future Transactions:**
   - New transactions will be added automatically when verified
   - No manual intervention needed

2. **SOA Generation:**
   - You can now generate statements for any account
   - Historical data is available immediately

3. **Balance Tracking:**
   - All balances are tracked from this point forward
   - Opening balances are accurate based on historical data

### Test the SOA Module

1. Navigate to **Statement of Accounts** in the UI
2. Select an account that has transactions
3. Choose a date range that includes the migrated transactions
4. Click **Generate Statement**
5. Verify the Excel file contains:
   - Correct opening balance
   - All transactions in the period
   - Accurate running balances
   - Correct closing balance

## 📝 Script Details

### What Gets Populated

For each verified transaction, the script populates:

| Field | Source | Logic |
|-------|--------|-------|
| Branch_ID | cust_acct_master or of_acct_master | Account's branch |
| ACC_No | tran_table.Account_No | Direct copy |
| TRAN_ID | tran_table.Tran_Id | Direct copy |
| TRAN_DATE | tran_table.Tran_Date | Direct copy |
| VALUE_DATE | tran_table.Value_Date | Direct copy |
| TRAN_SL_NO | Calculated | Auto-increment per transaction |
| NARRATION | tran_table.Narration | Direct copy |
| TRAN_TYPE | tran_table.Dr_Cr_Flag | D or C |
| TRAN_AMT | tran_table.LCY_Amt | Direct copy |
| Opening_Balance | Calculated | Previous closing balance |
| BALANCE_AFTER_TRAN | Calculated | Opening ± Amount |
| ENTRY_USER_ID | 'SYSTEM' | Migration marker |
| AUTH_USER_ID | 'MIGRATION' | Migration marker |
| CURRENCY_CODE | tran_table.Tran_Ccy or 'BDT' | Default BDT |
| GL_Num | sub_prod_master.Cum_GL_Num | From account's sub-product |
| RCRE_DATE | CURDATE() | Today's date |
| RCRE_TIME | CURTIME() | Current time |

### Performance Considerations

- **Indexes:** Created before population for optimal performance
- **Batch Processing:** Processes one transaction at a time
- **Memory Usage:** Uses temporary table for balance tracking
- **Rollback:** Wrapped in transaction (all or nothing)

## ✅ Final Checklist

Before considering the migration complete:

- [ ] Script executed without errors
- [ ] Table `txn_hist_acct` exists
- [ ] Record count matches expectations
- [ ] Sample records look correct
- [ ] Balances are sequential (no jumps)
- [ ] SOA generation works in UI
- [ ] Excel files contain correct data
- [ ] No errors in backend logs

---

## 🎉 Success!

Once the script completes successfully:

✅ **Table Created:** `txn_hist_acct` with proper structure  
✅ **Data Migrated:** All verified transactions populated  
✅ **Balances Calculated:** Accurate opening and closing balances  
✅ **SOA Ready:** Statement of Accounts module fully functional  

**You can now generate statements for any account with historical data!** 🚀

---

**Estimated Total Time:** 2-10 minutes (depending on data volume)  
**Difficulty:** Easy (just run the script)  
**Risk:** Low (script is safe and can be re-run)

