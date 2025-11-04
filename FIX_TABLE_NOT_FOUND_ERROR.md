# Fix: Table 'txn_hist_acct' doesn't exist

## 🔴 Error Message
```json
{
    "error": "Internal Server Error",
    "message": "An error occurred while generating the statement: JDBC exception executing SQL [SELECT * FROM txn_hist_acct WHERE ACC_No = ? AND TRAN_DATE < ? ORDER BY TRAN_DATE DESC, RCRE_TIME DESC LIMIT 1] [Table 'moneymarketdb.txn_hist_acct' doesn't exist] [n/a]; SQL [n/a]"
}
```

## 🔍 Root Cause
The `txn_hist_acct` table has not been created in the database yet. This table is required for the Statement of Accounts module to store transaction history.

## ✅ Solution: Create the Table

### Option 1: Using the Batch File (Easiest)

1. **Double-click** `create_soa_table.bat` in the project root directory
2. Enter your MySQL root password when prompted
3. The script will:
   - Create the table
   - Verify the table structure
   - Show the indexes

### Option 2: Using MySQL Command Line

1. **Open Command Prompt** or PowerShell
2. **Navigate to project directory:**
   ```bash
   cd "G:\Backup-Source Code\Money Market PTTP-reback"
   ```

3. **Run the SQL script:**
   ```bash
   mysql -u root -p moneymarketdb < create_txn_hist_acct_table.sql
   ```

4. **Enter your MySQL password** when prompted

### Option 3: Using MySQL Workbench (GUI)

1. **Open MySQL Workbench**
2. **Connect to your database**
3. **Open** `create_txn_hist_acct_table.sql` file
4. **Execute** the script (⚡ icon or Ctrl+Shift+Enter)

### Option 4: Manual SQL Execution

1. **Connect to MySQL:**
   ```bash
   mysql -u root -p moneymarketdb
   ```

2. **Copy and paste this SQL:**
   ```sql
   CREATE TABLE txn_hist_acct (
       -- Primary Key
       Hist_ID BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Unique history record identifier',
       
       -- Transaction Details
       Branch_ID VARCHAR(10) NOT NULL COMMENT 'Branch identifier',
       ACC_No VARCHAR(13) NOT NULL COMMENT 'Account number',
       TRAN_ID VARCHAR(20) NOT NULL COMMENT 'Foreign Key to tran_table(Tran_Id)',
       TRAN_DATE DATE NOT NULL COMMENT 'Transaction date',
       VALUE_DATE DATE NOT NULL COMMENT 'Value date',
       TRAN_SL_NO INT NOT NULL COMMENT 'Serial number for debit/credit leg',
       NARRATION VARCHAR(100) COMMENT 'Transaction description',
       TRAN_TYPE ENUM('D', 'C') NOT NULL COMMENT 'Debit or Credit',
       TRAN_AMT DECIMAL(20,2) NOT NULL COMMENT 'Transaction amount',
       
       -- Balance Information
       Opening_Balance DECIMAL(20,2) NOT NULL COMMENT 'Balance before this transaction',
       BALANCE_AFTER_TRAN DECIMAL(20,2) NOT NULL COMMENT 'Running balance after transaction',
       
       -- User & System Info
       ENTRY_USER_ID VARCHAR(20) NOT NULL COMMENT 'User who posted transaction',
       AUTH_USER_ID VARCHAR(20) COMMENT 'User who verified/authorized transaction',
       CURRENCY_CODE VARCHAR(3) DEFAULT 'BDT' COMMENT 'Transaction currency',
       GL_Num VARCHAR(9) COMMENT 'GL number from account sub-product',
       RCRE_DATE DATE NOT NULL COMMENT 'Record creation date',
       RCRE_TIME TIME NOT NULL COMMENT 'Record creation time',
       
       -- Indexes for performance
       INDEX idx_acc_no (ACC_No),
       INDEX idx_tran_date (TRAN_DATE),
       INDEX idx_acc_tran_date (ACC_No, TRAN_DATE),
       INDEX idx_tran_id (TRAN_ID),
       
       -- Foreign Key Constraint
       CONSTRAINT fk_txn_hist_tran_id 
           FOREIGN KEY (TRAN_ID) 
           REFERENCES tran_table(Tran_Id) 
           ON DELETE RESTRICT
           ON UPDATE CASCADE
           
   ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Transaction history for Statement of Accounts';
   ```

3. **Press Enter** to execute

## 🔍 Verify Table Creation

After creating the table, verify it was created successfully:

### Check Table Exists:
```sql
SHOW TABLES LIKE 'txn_hist_acct';
```
**Expected output:** 1 row showing `txn_hist_acct`

### Check Table Structure:
```sql
DESCRIBE txn_hist_acct;
```
**Expected output:** 18 columns listed

### Check Indexes:
```sql
SHOW INDEX FROM txn_hist_acct;
```
**Expected output:** 5 indexes including:
- PRIMARY (Hist_ID)
- idx_acc_no (ACC_No)
- idx_tran_date (TRAN_DATE)
- idx_acc_tran_date (ACC_No, TRAN_DATE) ← Most important for performance
- idx_tran_id (TRAN_ID)

### Check Foreign Key:
```sql
SELECT 
    CONSTRAINT_NAME,
    TABLE_NAME,
    COLUMN_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
WHERE TABLE_NAME = 'txn_hist_acct' 
AND CONSTRAINT_NAME = 'fk_txn_hist_tran_id';
```
**Expected output:** 1 row showing FK relationship to `tran_table`

## 🔄 After Table Creation

### Step 1: Restart Backend (if running)
If your backend is running, restart it to ensure it picks up the new table:

```bash
# If running as a service
sudo systemctl restart moneymarket

# If running in IDE, just stop and restart
```

### Step 2: Test Transaction History Population

1. **Create a test transaction** via the UI or API
2. **Post the transaction**
3. **Verify the transaction** (this triggers history population)
4. **Check the table:**
   ```sql
   SELECT * FROM txn_hist_acct ORDER BY RCRE_DATE DESC, RCRE_TIME DESC LIMIT 5;
   ```

**Expected:** You should see records appear after verifying transactions

### Step 3: Test SOA Generation Again

1. Navigate to **Statement of Accounts** in the UI
2. Select an account
3. Choose a date range
4. Click **Generate Statement**

**Expected:** Excel file should download successfully!

## 📊 Understanding the Table

### Purpose
The `txn_hist_acct` table stores a complete history of all account transactions for Statement of Accounts generation.

### When It's Populated
- **Automatically** when a transaction is verified
- **Real-time** - no manual intervention needed
- **For both legs** of a transaction (debit and credit)

### Key Features
- **Running Balance:** Each record includes opening and closing balance
- **Multi-leg Support:** TRAN_SL_NO tracks multiple legs of same transaction
- **Audit Trail:** Records who entered and who authorized
- **Performance:** Composite index on (ACC_No, TRAN_DATE) ensures fast queries

## 🐛 Troubleshooting

### Issue: "Access denied" when creating table
**Solution:** Make sure you're using the root user or a user with CREATE TABLE privileges

### Issue: "Foreign key constraint fails"
**Cause:** `tran_table` doesn't exist or `Tran_Id` column doesn't exist  
**Solution:** 
1. Check if `tran_table` exists: `SHOW TABLES LIKE 'tran_table';`
2. If not, you need to create it first
3. Or temporarily remove the foreign key constraint

### Issue: Table created but still getting error
**Solution:** 
1. Restart the backend application
2. Check the table name is exactly `txn_hist_acct` (case-sensitive on Linux)
3. Verify you're connected to the correct database

### Issue: Records not populating after transaction verification
**Cause:** Backend code not deployed or error in population logic  
**Solution:**
1. Check backend logs for errors
2. Verify backend has been rebuilt with new code
3. Check `TransactionService.verifyTransaction()` is calling `TransactionHistoryService`

## ✅ Success Criteria

You'll know everything is working when:

1. ✅ Table exists in database
2. ✅ Table has 5 indexes
3. ✅ Foreign key constraint is active
4. ✅ Records appear after verifying transactions
5. ✅ SOA generation downloads Excel file
6. ✅ Excel contains transaction data with accurate balances

## 📝 Quick Command Reference

```bash
# Create table
mysql -u root -p moneymarketdb < create_txn_hist_acct_table.sql

# Verify table
mysql -u root -p moneymarketdb -e "DESCRIBE txn_hist_acct;"

# Check records
mysql -u root -p moneymarketdb -e "SELECT COUNT(*) FROM txn_hist_acct;"

# View recent records
mysql -u root -p moneymarketdb -e "SELECT * FROM txn_hist_acct ORDER BY RCRE_DATE DESC LIMIT 5;"

# Drop table (if you need to recreate)
mysql -u root -p moneymarketdb -e "DROP TABLE IF EXISTS txn_hist_acct;"
```

## 🎯 Next Steps After Fix

1. ✅ Create table (you're doing this now)
2. ✅ Verify table structure
3. ✅ Restart backend (if needed)
4. ✅ Verify a test transaction
5. ✅ Check transaction history populated
6. ✅ Test SOA generation
7. ✅ Verify Excel content

---

**Estimated Time to Fix:** 2-5 minutes  
**Difficulty:** Easy  
**Impact:** High (enables entire SOA module)

Once the table is created, the SOA module will work perfectly! 🎉

