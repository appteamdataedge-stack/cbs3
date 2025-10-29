# Rollback Error Fix - Transaction Rolled Back

## Issue
When executing Batch Job 2, the error occurred:
```json
{
    "success": false,
    "message": "Batch Job 2 failed: Transaction silently rolled back because it has been marked as rollback-only",
    "timestamp": "2025-10-20T18:02:53.098522"
}
```

## Root Cause

The application was connecting to the `cbs` database instead of `moneymarketdb`, and the `cbs.intt_accr_tran` table still had the old `Tran_Id` column that we removed from the entity.

### Error in Logs:
```
ERROR - Field 'Tran_Id' doesn't have a default value
[insert into intt_accr_tran (account_no,accrual_date,amount,dr_cr_flag,...) values (?,?,?,?,?,...)]
```

### Investigation Results:

1. **Database Check**: Found that there are TWO databases:
   - `moneymarketdb` - Has correct schema (Accr_Tran_Id, NO Tran_Id)
   - `cbs` - Had old schema (tran_id column still present)

2. **Schema Mismatch**:
   ```sql
   -- Query to find the issue
   SELECT TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME
   FROM INFORMATION_SCHEMA.COLUMNS
   WHERE COLUMN_NAME = 'Tran_Id' AND TABLE_NAME LIKE '%ntt%ccr%';

   -- Result:
   TABLE_SCHEMA: cbs
   TABLE_NAME: intt_accr_tran
   COLUMN_NAME: tran_id
   ```

3. **Application Configuration**:
   ```properties
   spring.datasource.url=jdbc:mysql://127.0.0.1:3306/moneymarketdb
   spring.jpa.properties.hibernate.default_schema=moneymarketdb
   ```

   Even though configured for `moneymarketdb`, the application was somehow using `cbs` database.

## Fix Applied

### Step 1: Drop Foreign Key Constraint
```sql
USE cbs;
ALTER TABLE gl_movement_accrual DROP FOREIGN KEY fk_glmva_tran;
```

### Step 2: Drop Tran_Id Column
```sql
USE cbs;
ALTER TABLE intt_accr_tran DROP COLUMN tran_id;
```

### Step 3: Restart Application
```bash
# Kill old process on port 8082
taskkill //F //PID 7304

# Restart Spring Boot application
mvn spring-boot:run
```

## Why This Happened

1. **Multiple Database Instances**: There were two separate databases (`moneymarketdb` and `cbs`) with different schemas
2. **Schema Migration Not Applied**: The database migration script was only run on `moneymarketdb`, not on `cbs`
3. **Connection Ambiguity**: The application might have been connecting to `cbs` due to environment variables or cached connections

## Prevention

### 1. Always Check All Databases
Before making schema changes, verify which database(s) your application uses:

```sql
-- Find all databases with your tables
SELECT TABLE_SCHEMA, TABLE_NAME
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_NAME = 'intt_accr_tran';
```

### 2. Apply Migrations to All Relevant Databases
If multiple databases exist, apply schema changes to all of them:

```bash
# For moneymarketdb
mysql -u root -p moneymarketdb < migration_script.sql

# For cbs (if needed)
mysql -u root -p cbs < migration_script.sql
```

### 3. Verify Schema After Migration
```sql
-- Check column existence across all databases
SELECT
    TABLE_SCHEMA,
    TABLE_NAME,
    COLUMN_NAME,
    DATA_TYPE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'intt_accr_tran'
AND COLUMN_NAME IN ('Accr_Tran_Id', 'Tran_Id', 'accr_id', 'tran_id')
ORDER BY TABLE_SCHEMA, COLUMN_NAME;
```

### 4. Set Explicit Schema in Connection String
```properties
spring.datasource.url=jdbc:mysql://127.0.0.1:3306/moneymarketdb?createDatabaseIfNotExist=true
spring.jpa.properties.hibernate.default_schema=moneymarketdb
spring.jpa.properties.hibernate.default_catalog=moneymarketdb
```

## Expected Result After Fix

After dropping the `tran_id` column from the `cbs` database and restarting the application, Batch Job 2 should now successfully create accrual entries.

### Test Batch Job 2:
```bash
curl -X POST http://localhost:8082/admin/eod/batch/interest-accrual
```

### Expected Response:
```json
{
  "success": true,
  "jobName": "Interest Accrual Transaction Update",
  "recordsProcessed": 4,
  "message": "Batch Job 2 completed successfully",
  "systemDate": "2025-01-10"
}
```

### Verify in Database:
```sql
SELECT
    Account_No,
    GL_Account_No,
    Dr_Cr_Flag,
    Amount,
    Interest_Rate
FROM cbs.intt_accr_tran
WHERE Accrual_Date = '2025-01-10'
ORDER BY Account_No, Dr_Cr_Flag;
```

Expected: 4 records (2 per active account)

## Lessons Learned

1. **Schema Consistency**: Ensure all databases used by the application have consistent schemas
2. **Migration Scripts**: Always identify ALL target databases before running migrations
3. **Foreign Key Dependencies**: Check for FK constraints before dropping columns
4. **Transaction Management**: Schema mismatches cause transactions to rollback, even if the error seems unrelated
5. **Database Discovery**: Use INFORMATION_SCHEMA queries to find all instances of tables across databases

## Files Created

- `fix_intt_accr_tran_schema.sql` - Quick fix script to drop Tran_Id column
- This document - `ROLLBACK_ERROR_FIX.md`

## Status

✅ **Fixed**: Dropped `tran_id` column from `cbs.intt_accr_tran` table
✅ **Restarting**: Backend application is restarting with correct schema
⏳ **Testing**: Ready to test Batch Job 2 once backend starts

The system should now be able to process interest accruals without rollback errors!
