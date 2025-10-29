-- =====================================================
-- EOD Batch Jobs Implementation - Database Migration
-- Date: 2025-01-XX
-- Description: Add missing columns and fix schema issues for comprehensive EOD processing
-- =====================================================

USE moneymarketdb;

-- =====================================================
-- 1. Fix Intt_Accr_Tran table schema
-- Critical corrections for Batch Job 2
-- =====================================================

-- Step 1a: Drop foreign key constraint from GL_Movement_Accrual (if exists)
SET @fk_exists = (SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = 'moneymarketdb'
    AND TABLE_NAME = 'GL_Movement_Accrual'
    AND CONSTRAINT_NAME = 'gl_movement_accrual_ibfk_2');

SET @sql_drop_fk = IF(@fk_exists > 0,
    'ALTER TABLE GL_Movement_Accrual DROP FOREIGN KEY gl_movement_accrual_ibfk_2',
    'SELECT "FK constraint does not exist, skipping..." AS Info');

PREPARE stmt FROM @sql_drop_fk;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 1b: Drop Tran_Id column and its foreign key from Intt_Accr_Tran
SET @tran_fk_exists = (SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = 'moneymarketdb'
    AND TABLE_NAME = 'Intt_Accr_Tran'
    AND CONSTRAINT_NAME = 'intt_accr_tran_ibfk_2');

SET @sql_drop_tran_fk = IF(@tran_fk_exists > 0,
    'ALTER TABLE Intt_Accr_Tran DROP FOREIGN KEY intt_accr_tran_ibfk_2',
    'SELECT "Tran_Id FK does not exist, skipping..." AS Info');

PREPARE stmt FROM @sql_drop_tran_fk;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Drop Tran_Id column (no longer needed)
SET @tran_col_exists = (SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'moneymarketdb'
    AND TABLE_NAME = 'Intt_Accr_Tran'
    AND COLUMN_NAME = 'Tran_Id');

SET @sql_drop_tran_col = IF(@tran_col_exists > 0,
    'ALTER TABLE Intt_Accr_Tran DROP COLUMN Tran_Id',
    'SELECT "Tran_Id column does not exist, skipping..." AS Info');

PREPARE stmt FROM @sql_drop_tran_col;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 1c: Rename Accr_Id to Accr_Tran_Id in Intt_Accr_Tran
ALTER TABLE Intt_Accr_Tran
CHANGE COLUMN Accr_Id Accr_Tran_Id BIGINT NOT NULL AUTO_INCREMENT
  COMMENT 'Unique identifier for interest accrual transaction';

-- Step 1d: Update GL_Movement_Accrual to reference Accr_Tran_Id
ALTER TABLE GL_Movement_Accrual
CHANGE COLUMN Accr_Id Accr_Tran_Id BIGINT NOT NULL
  COMMENT 'Foreign key to Intt_Accr_Tran.Accr_Tran_Id';

-- Step 1e: Recreate foreign key constraint with new column name
ALTER TABLE GL_Movement_Accrual
ADD CONSTRAINT gl_movement_accrual_ibfk_2
  FOREIGN KEY (Accr_Tran_Id) REFERENCES Intt_Accr_Tran(Accr_Tran_Id)
  ON DELETE RESTRICT
  ON UPDATE CASCADE;

-- =====================================================
-- 2. Update Sub_Prod_Master table
-- Add Interest GL account columns
-- =====================================================

ALTER TABLE Sub_Prod_Master
ADD COLUMN IF NOT EXISTS Interest_Expenditure_GL_Num VARCHAR(20) NULL
  COMMENT 'GL account for interest expenditure (liability accounts)'
  AFTER Interest_Payable_GL_Num;

ALTER TABLE Sub_Prod_Master
ADD COLUMN IF NOT EXISTS Interest_Receivable_GL_Num VARCHAR(20) NULL
  COMMENT 'GL account for interest receivable (asset accounts)'
  AFTER Interest_Income_GL_Num;

-- =====================================================
-- 2. Verify existing tables have required columns
-- The following tables should already have these columns from BRD PTTP02V1.0
-- This section is for verification and documentation
-- =====================================================

-- Intt_Accr_Tran should have:
-- - Tran_Date, Value_Date, Dr_Cr_Flag, Tran_Status
-- - GL_Account_No, Tran_Ccy, FCY_Amt, Exchange_Rate, LCY_Amt, Narration

-- GL_Movement_Accrual should have:
-- - Tran_Date, Tran_Id, Tran_Ccy, FCY_Amt, Exchange_Rate, LCY_Amt, Narration

-- Acct_Bal_Accrual should have:
-- - Tran_Date, Opening_Bal, DR_Summation, CR_Summation, Closing_Bal

-- GL_Balance should have:
-- - Tran_Date, Opening_Bal, DR_Summation, CR_Summation, Closing_Bal

-- =====================================================
-- 3. Create indices for performance optimization
-- =====================================================

-- Index on Intt_Accr_Tran for Batch Job 2 & 3
CREATE INDEX IF NOT EXISTS idx_intt_accr_tran_accrual_date
  ON Intt_Accr_Tran(Accrual_Date);

CREATE INDEX IF NOT EXISTS idx_intt_accr_tran_account_accrual
  ON Intt_Accr_Tran(Account_No, Accrual_Date);

CREATE INDEX IF NOT EXISTS idx_intt_accr_tran_gl_account
  ON Intt_Accr_Tran(GL_Account_No);

CREATE INDEX IF NOT EXISTS idx_intt_accr_tran_id
  ON Intt_Accr_Tran(Accr_Tran_Id);

-- Index on GL_Movement_Accrual for Batch Job 3 & 5
CREATE INDEX IF NOT EXISTS idx_gl_mov_accr_accrual_date
  ON GL_Movement_Accrual(Accrual_Date);

CREATE INDEX IF NOT EXISTS idx_gl_mov_accr_gl_date
  ON GL_Movement_Accrual(GL_Num, Accrual_Date);

-- Index on Tran_Table for Batch Job 4
CREATE INDEX IF NOT EXISTS idx_tran_table_date_status
  ON Tran_Table(Tran_Date, Tran_Status);

CREATE INDEX IF NOT EXISTS idx_tran_table_account_date
  ON Tran_Table(Account_No, Tran_Date);

-- Index on GL_Movement for Batch Job 4 & 5
CREATE INDEX IF NOT EXISTS idx_gl_movement_tran_date
  ON GL_Movement(Tran_Date);

CREATE INDEX IF NOT EXISTS idx_gl_movement_gl_date
  ON GL_Movement(GL_Num, Tran_Date);

CREATE INDEX IF NOT EXISTS idx_gl_movement_tran_id
  ON GL_Movement(Tran_Id);

-- Index on GL_Balance for Batch Job 5 & 7
CREATE INDEX IF NOT EXISTS idx_gl_balance_tran_date
  ON GL_Balance(Tran_Date);

CREATE INDEX IF NOT EXISTS idx_gl_balance_gl_date
  ON GL_Balance(GL_Num, Tran_Date);

-- Index on Acct_Bal_Accrual for Batch Job 6
CREATE INDEX IF NOT EXISTS idx_acct_bal_accr_tran_date
  ON Acct_Bal_Accrual(Tran_Date);

CREATE INDEX IF NOT EXISTS idx_acct_bal_accr_account_date
  ON Acct_Bal_Accrual(Account_No, Tran_Date);

-- Index on OF_Acct_Master for office account lookups
CREATE INDEX IF NOT EXISTS idx_of_acct_gl_status
  ON OF_Acct_Master(GL_Num, Account_Status);

-- =====================================================
-- 4. Sample data update script (optional)
-- Update existing sub-products with GL account mappings
-- =====================================================

-- Example: Update a liability sub-product with interest GL accounts
-- UPDATE Sub_Prod_Master
-- SET Interest_Payable_GL_Num = '120101001',
--     Interest_Expenditure_GL_Num = '140101001',
--     Interest_Income_GL_Num = NULL,
--     Interest_Receivable_GL_Num = NULL
-- WHERE Sub_Product_Code = 'SAV001' -- Savings account (liability)
--   AND Product_Id IN (SELECT Product_Id FROM Prod_Master WHERE Product_Type = 'Liability');

-- Example: Update an asset sub-product with interest GL accounts
-- UPDATE Sub_Prod_Master
-- SET Interest_Payable_GL_Num = NULL,
--     Interest_Expenditure_GL_Num = NULL,
--     Interest_Income_GL_Num = '140201001',
--     Interest_Receivable_GL_Num = '210201001'
-- WHERE Sub_Product_Code = 'LOAN001' -- Loan account (asset)
--   AND Product_Id IN (SELECT Product_Id FROM Prod_Master WHERE Product_Type = 'Asset');

-- =====================================================
-- 5. Verification queries
-- Run these to verify the migration was successful
-- =====================================================

-- Verify Intt_Accr_Tran schema changes
SELECT
  COLUMN_NAME,
  COLUMN_TYPE,
  IS_NULLABLE,
  COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'moneymarketdb'
  AND TABLE_NAME = 'Intt_Accr_Tran'
  AND COLUMN_NAME IN ('Accr_Tran_Id', 'Tran_Id')
ORDER BY COLUMN_NAME;

-- Expected result: Accr_Tran_Id exists, Tran_Id should NOT exist

-- Verify GL_Movement_Accrual FK update
SELECT
  COLUMN_NAME,
  COLUMN_TYPE,
  IS_NULLABLE,
  COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'moneymarketdb'
  AND TABLE_NAME = 'GL_Movement_Accrual'
  AND COLUMN_NAME = 'Accr_Tran_Id';

-- Expected result: Accr_Tran_Id column exists with BIGINT type

-- Verify Sub_Prod_Master columns
SELECT
  COLUMN_NAME,
  COLUMN_TYPE,
  IS_NULLABLE,
  COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'moneymarketdb'
  AND TABLE_NAME = 'Sub_Prod_Master'
  AND COLUMN_NAME IN ('Interest_Expenditure_GL_Num', 'Interest_Receivable_GL_Num');

-- Check indices created
SELECT
  TABLE_NAME,
  INDEX_NAME,
  GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) as INDEX_COLUMNS
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = 'moneymarketdb'
  AND INDEX_NAME LIKE 'idx_%'
GROUP BY TABLE_NAME, INDEX_NAME
ORDER BY TABLE_NAME, INDEX_NAME;

-- =====================================================
-- 6. Rollback script (use with caution)
-- =====================================================

/*
-- WARNING: This rollback script will restore the previous schema
-- It will recreate the Tran_Id column and revert Accr_Tran_Id back to Accr_Id
-- Only use if absolutely necessary!

-- Step 1: Drop FK constraint from GL_Movement_Accrual
ALTER TABLE GL_Movement_Accrual DROP FOREIGN KEY gl_movement_accrual_ibfk_2;

-- Step 2: Revert column names
ALTER TABLE GL_Movement_Accrual
CHANGE COLUMN Accr_Tran_Id Accr_Id BIGINT NOT NULL;

ALTER TABLE Intt_Accr_Tran
CHANGE COLUMN Accr_Tran_Id Accr_Id BIGINT NOT NULL AUTO_INCREMENT;

-- Step 3: Re-add Tran_Id column
ALTER TABLE Intt_Accr_Tran
ADD COLUMN Tran_Id VARCHAR(20) NULL AFTER Accr_Id;

-- Step 4: Recreate FK constraints
ALTER TABLE GL_Movement_Accrual
ADD CONSTRAINT gl_movement_accrual_ibfk_2
  FOREIGN KEY (Accr_Id) REFERENCES Intt_Accr_Tran(Accr_Id);

-- Note: You may need to recreate the Tran_Id FK if it was referencing Tran_Table

-- Rollback: Remove added columns from Sub_Prod_Master
ALTER TABLE Sub_Prod_Master DROP COLUMN Interest_Expenditure_GL_Num;
ALTER TABLE Sub_Prod_Master DROP COLUMN Interest_Receivable_GL_Num;

-- Rollback: Drop indices
DROP INDEX IF EXISTS idx_intt_accr_tran_accrual_date ON Intt_Accr_Tran;
DROP INDEX IF EXISTS idx_intt_accr_tran_account_accrual ON Intt_Accr_Tran;
DROP INDEX IF EXISTS idx_intt_accr_tran_gl_account ON Intt_Accr_Tran;
DROP INDEX IF EXISTS idx_intt_accr_tran_id ON Intt_Accr_Tran;
DROP INDEX IF EXISTS idx_gl_mov_accr_accrual_date ON GL_Movement_Accrual;
DROP INDEX IF EXISTS idx_gl_mov_accr_gl_date ON GL_Movement_Accrual;
DROP INDEX IF EXISTS idx_tran_table_date_status ON Tran_Table;
DROP INDEX IF EXISTS idx_tran_table_account_date ON Tran_Table;
DROP INDEX IF EXISTS idx_gl_movement_tran_date ON GL_Movement;
DROP INDEX IF EXISTS idx_gl_movement_gl_date ON GL_Movement;
DROP INDEX IF EXISTS idx_gl_movement_tran_id ON GL_Movement;
DROP INDEX IF EXISTS idx_gl_balance_tran_date ON GL_Balance;
DROP INDEX IF EXISTS idx_gl_balance_gl_date ON GL_Balance;
DROP INDEX IF EXISTS idx_acct_bal_accr_tran_date ON Acct_Bal_Accrual;
DROP INDEX IF EXISTS idx_acct_bal_accr_account_date ON Acct_Bal_Accrual;
DROP INDEX IF EXISTS idx_of_acct_gl_status ON OF_Acct_Master;
*/

-- =====================================================
-- End of migration script
-- =====================================================
