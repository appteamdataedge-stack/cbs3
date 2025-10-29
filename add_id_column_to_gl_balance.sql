-- ==================================================================
-- Migration: Add Auto-Increment Id Column to GL_Balance Table
-- Purpose: Replace composite primary key (GL_Num, Tran_date) with
--          single auto-increment Id to simplify relationships and queries
-- Date: 2025-10-23
-- ==================================================================

USE moneymarketdb;

-- ==================================================================
-- BACKUP RECOMMENDATION
-- ==================================================================
-- IMPORTANT: Before running this migration, backup your gl_balance table:
-- CREATE TABLE gl_balance_backup AS SELECT * FROM gl_balance;

-- ==================================================================
-- Step 1: Drop foreign key constraint FROM gl_balance
-- ==================================================================
-- gl_balance has a foreign key TO gl_setup, we need to drop it first
ALTER TABLE gl_balance DROP FOREIGN KEY gl_balance_ibfk_1;

-- ==================================================================
-- Step 2: Drop the existing composite primary key
-- ==================================================================
ALTER TABLE gl_balance DROP PRIMARY KEY;

-- ==================================================================
-- Step 3: Add the new Id column as auto-increment primary key
-- ==================================================================
ALTER TABLE gl_balance
ADD COLUMN Id BIGINT AUTO_INCREMENT PRIMARY KEY FIRST;

-- ==================================================================
-- Step 4: Add unique constraint on (GL_Num, Tran_date) to prevent duplicates
-- ==================================================================
-- This ensures we still maintain data integrity - no duplicate GL records per date
ALTER TABLE gl_balance
ADD CONSTRAINT uq_gl_balance_gl_num_tran_date UNIQUE (GL_Num, Tran_date);

-- ==================================================================
-- Step 4b: Re-add the foreign key constraint to gl_setup
-- ==================================================================
ALTER TABLE gl_balance
ADD CONSTRAINT gl_balance_ibfk_1
FOREIGN KEY (GL_Num) REFERENCES gl_setup(GL_Num);

-- ==================================================================
-- Step 5: Add index on GL_Num for foreign key relationship
-- ==================================================================
-- This improves query performance when joining/filtering by GL_Num
CREATE INDEX idx_gl_balance_gl_num ON gl_balance(GL_Num);

-- ==================================================================
-- Step 6: Add index on Tran_date for date-based queries
-- ==================================================================
-- This improves query performance for date range queries
CREATE INDEX idx_gl_balance_tran_date ON gl_balance(Tran_date);

-- ==================================================================
-- Step 7: Verify the changes
-- ==================================================================
SHOW CREATE TABLE gl_balance\G

-- Check the indexes
SHOW INDEX FROM gl_balance;

-- Display sample data to verify Id column is populated
SELECT Id, GL_Num, Tran_date, Opening_Bal, Closing_Bal, Last_Updated
FROM gl_balance
ORDER BY Id DESC
LIMIT 10;

-- ==================================================================
-- Step 8: Verify data integrity
-- ==================================================================
-- Check for any null Ids (should be none)
SELECT COUNT(*) AS null_id_count
FROM gl_balance
WHERE Id IS NULL;

-- Check for any duplicate (GL_Num, Tran_date) combinations (should be none)
SELECT GL_Num, Tran_date, COUNT(*) AS duplicate_count
FROM gl_balance
GROUP BY GL_Num, Tran_date
HAVING COUNT(*) > 1;

-- ==================================================================
-- Step 9: Display summary
-- ==================================================================
SELECT
    'Migration completed successfully!' AS Status,
    'gl_balance now has:' AS Changes,
    '1. Auto-increment Id as PRIMARY KEY' AS Change_1,
    '2. UNIQUE constraint on (GL_Num, Tran_date)' AS Change_2,
    '3. Index on GL_Num for performance' AS Change_3,
    '4. Index on Tran_date for performance' AS Change_4;

-- ==================================================================
-- ROLLBACK SCRIPT (if needed)
-- ==================================================================
-- If you need to rollback this migration:
/*
USE moneymarketdb;

-- Drop the new Id column
ALTER TABLE gl_balance DROP COLUMN Id;

-- Drop the unique constraint
ALTER TABLE gl_balance DROP INDEX uq_gl_balance_gl_num_tran_date;

-- Drop the indexes
DROP INDEX idx_gl_balance_gl_num ON gl_balance;
DROP INDEX idx_gl_balance_tran_date ON gl_balance;

-- Re-add the composite primary key
ALTER TABLE gl_balance ADD PRIMARY KEY (GL_Num, Tran_date);

-- Restore data from backup if needed
-- DELETE FROM gl_balance;
-- INSERT INTO gl_balance SELECT * FROM gl_balance_backup;
*/

-- ==================================================================
-- TESTING QUERIES
-- ==================================================================
-- After migration, test these queries to ensure everything works:

-- Test 1: Insert a new balance record (Id should auto-increment)
/*
INSERT INTO gl_balance (GL_Num, Tran_date, Opening_Bal, DR_Summation, CR_Summation, Closing_Bal, Current_Balance, Last_Updated)
VALUES ('999999999', '2025-10-23', 0.00, 100.00, 50.00, 50.00, 50.00, NOW());

-- Verify the Id was auto-generated
SELECT * FROM gl_balance WHERE GL_Num = '999999999' AND Tran_date = '2025-10-23';

-- Clean up test data
DELETE FROM gl_balance WHERE GL_Num = '999999999';
*/

-- Test 2: Verify unique constraint prevents duplicates
/*
INSERT INTO gl_balance (GL_Num, Tran_date, Opening_Bal, DR_Summation, CR_Summation, Closing_Bal, Current_Balance, Last_Updated)
VALUES ('999999999', '2025-10-23', 0.00, 100.00, 50.00, 50.00, 50.00, NOW());

-- This should fail with duplicate key error:
INSERT INTO gl_balance (GL_Num, Tran_date, Opening_Bal, DR_Summation, CR_Summation, Closing_Bal, Current_Balance, Last_Updated)
VALUES ('999999999', '2025-10-23', 0.00, 200.00, 100.00, 100.00, 100.00, NOW());

-- Clean up
DELETE FROM gl_balance WHERE GL_Num = '999999999';
*/

-- Test 3: Verify indexes are being used
/*
EXPLAIN SELECT * FROM gl_balance WHERE GL_Num = '110101001';
EXPLAIN SELECT * FROM gl_balance WHERE Tran_date = '2025-01-12';
EXPLAIN SELECT * FROM gl_balance WHERE GL_Num = '110101001' AND Tran_date = '2025-01-12';
*/
