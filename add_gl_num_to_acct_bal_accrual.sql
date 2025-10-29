-- =====================================================
-- Migration: Add GL_Num Column to acct_bal_accrual
-- Purpose: Determine account type for conditional DR/CR summation logic
-- Date: 2025-01-22
-- =====================================================

USE moneymarketdb;

-- Step 1: Add GL_Num column to acct_bal_accrual table
ALTER TABLE acct_bal_accrual
ADD COLUMN GL_Num VARCHAR(9) AFTER Account_No;

-- Step 2: Add index for better query performance
CREATE INDEX idx_acct_bal_accrual_gl_num ON acct_bal_accrual(GL_Num);

-- Step 3: Populate GL_Num for existing records
UPDATE acct_bal_accrual aba
INNER JOIN cust_acct_master cam ON aba.Account_No = cam.Account_No
INNER JOIN sub_prod_master spm ON cam.Sub_Product_Id = spm.Sub_Product_Id
SET aba.GL_Num = spm.Cum_GL_Num
WHERE aba.GL_Num IS NULL;

-- Step 4: Fix DR_Summation for liability accounts (GL starts with '1')
-- Liability accounts should never have debit accruals
UPDATE acct_bal_accrual
SET DR_Summation = 0
WHERE GL_Num LIKE '1%';

-- Step 5: Fix CR_Summation for asset accounts (GL starts with '2')
-- Asset accounts should never have credit accruals
UPDATE acct_bal_accrual
SET CR_Summation = 0
WHERE GL_Num LIKE '2%';

-- Step 6: Verify the migration
SELECT
    SUBSTRING(GL_Num, 1, 1) as GL_Type,
    COUNT(*) as Record_Count,
    SUM(CASE WHEN DR_Summation = 0 THEN 1 ELSE 0 END) as Zero_DR_Count,
    SUM(CASE WHEN CR_Summation = 0 THEN 1 ELSE 0 END) as Zero_CR_Count
FROM acct_bal_accrual
WHERE GL_Num IS NOT NULL
GROUP BY SUBSTRING(GL_Num, 1, 1);

-- Expected result:
-- GL_Type='1' (Liability): All records should have DR_Summation=0
-- GL_Type='2' (Asset): All records should have CR_Summation=0

SELECT 'Migration completed successfully!' as Status;
