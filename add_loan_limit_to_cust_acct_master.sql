-- =====================================================
-- Migration Script: Add Loan/Limit Amount Field
-- =====================================================
-- Purpose: Add loan_limit column to cust_acct_master table
--          for Asset-side customer accounts (GL starting with "2")
-- Date: October 28, 2025
-- =====================================================

USE moneymarket;

-- Step 1: Add loan_limit column to cust_acct_master table
ALTER TABLE Cust_Acct_Master 
ADD COLUMN Loan_Limit DECIMAL(18, 2) DEFAULT 0.00 
COMMENT 'Loan/Limit Amount for Asset-side customer accounts (GL starting with 2)';

-- Step 2: Verify the column was added
SELECT 
    COLUMN_NAME, 
    DATA_TYPE, 
    COLUMN_DEFAULT, 
    IS_NULLABLE,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'moneymarket'
  AND TABLE_NAME = 'Cust_Acct_Master'
  AND COLUMN_NAME = 'Loan_Limit';

-- Step 3: Update existing Asset-side accounts with default loan limit (optional)
-- This sets a default loan limit for existing asset accounts
-- Comment out if you want all existing accounts to remain at 0
/*
UPDATE Cust_Acct_Master
SET Loan_Limit = 0.00
WHERE GL_Num LIKE '2%'
  AND Loan_Limit IS NULL;
*/

-- Step 4: Verification queries
-- Check all accounts with their loan limits
SELECT 
    Account_No,
    Acct_Name,
    GL_Num,
    Loan_Limit,
    Account_Status,
    CASE 
        WHEN GL_Num LIKE '2%' THEN 'Asset Account (Loan/Advance)'
        WHEN GL_Num LIKE '1%' THEN 'Liability Account (Deposit)'
        ELSE 'Other'
    END AS Account_Type
FROM Cust_Acct_Master
ORDER BY GL_Num, Account_No;

-- Check Asset accounts specifically
SELECT 
    Account_No,
    Acct_Name,
    GL_Num,
    Loan_Limit,
    Account_Status
FROM Cust_Acct_Master
WHERE GL_Num LIKE '2%'
ORDER BY Account_No;

-- Step 5: Sample data for testing (optional)
-- Uncomment to add test loan limits to existing asset accounts
/*
-- Example: Set loan limit for specific loan accounts
UPDATE Cust_Acct_Master
SET Loan_Limit = 100000.00
WHERE Account_No = '200000001234'  -- Replace with actual account number
  AND GL_Num LIKE '2%';

UPDATE Cust_Acct_Master
SET Loan_Limit = 500000.00
WHERE Account_No = '200000005678'  -- Replace with actual account number
  AND GL_Num LIKE '2%';
*/

-- =====================================================
-- ROLLBACK SCRIPT (if needed)
-- =====================================================
-- To rollback this migration, run:
-- ALTER TABLE Cust_Acct_Master DROP COLUMN Loan_Limit;
-- =====================================================

-- Success message
SELECT 'Migration completed successfully! Loan_Limit column added to Cust_Acct_Master table.' AS Status;

