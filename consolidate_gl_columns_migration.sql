-- =====================================================
-- GL Columns Consolidation Migration
-- =====================================================
-- Purpose: Consolidate 4 separate GL columns into 2 consolidated columns
--
-- FROM (4 columns):
--   - interest_income_gl_num
--   - interest_expenditure_gl_num
--   - interest_receivable_gl_num
--   - interest_payable_gl_num
--
-- TO (2 columns):
--   - interest_income_expenditure_gl_num (stores either income OR expenditure)
--   - interest_receivable_payable_gl_num (stores either receivable OR payable)
--
-- Business Logic:
--   - For LIABILITY products (Cum_GL_Num starts with '1'): stores expenditure + payable
--   - For ASSET products (Cum_GL_Num starts with '2'): stores income + receivable
-- =====================================================

USE moneymarketdb;

-- =====================================================
-- STEP 1: Add new consolidated columns
-- =====================================================

ALTER TABLE sub_prod_master
ADD COLUMN interest_income_expenditure_gl_num VARCHAR(20) NULL AFTER interest_increment,
ADD COLUMN interest_receivable_payable_gl_num VARCHAR(20) NULL AFTER interest_income_expenditure_gl_num;

-- =====================================================
-- STEP 2: Migrate existing data to new columns
-- =====================================================

-- For each record, determine if it's LIABILITY or ASSET based on Cum_GL_Num
-- Then copy appropriate values from old columns to new columns

-- Migrate LIABILITY products (Cum_GL_Num starts with '1')
UPDATE sub_prod_master
SET
    -- For liabilities: expenditure goes to income_expenditure column
    interest_income_expenditure_gl_num = NULLIF(TRIM(COALESCE(interest_expenditure_gl_num, '')), ''),
    -- For liabilities: payable goes to receivable_payable column
    interest_receivable_payable_gl_num = NULLIF(TRIM(COALESCE(interest_payable_gl_num, '')), '')
WHERE Cum_GL_Num LIKE '1%';

-- Migrate ASSET products (Cum_GL_Num starts with '2')
UPDATE sub_prod_master
SET
    -- For assets: income goes to income_expenditure column
    interest_income_expenditure_gl_num = NULLIF(TRIM(COALESCE(interest_income_gl_num, '')), ''),
    -- For assets: receivable goes to receivable_payable column
    interest_receivable_payable_gl_num = NULLIF(TRIM(COALESCE(interest_receivable_gl_num, '')), '')
WHERE Cum_GL_Num LIKE '2%';

-- =====================================================
-- STEP 3: Verify migration
-- =====================================================

SELECT
    Sub_Product_Id,
    Sub_Product_Code,
    Cum_GL_Num,
    CASE
        WHEN Cum_GL_Num LIKE '1%' THEN 'LIABILITY'
        WHEN Cum_GL_Num LIKE '2%' THEN 'ASSET'
        ELSE 'UNKNOWN'
    END as Product_Type,
    interest_income_expenditure_gl_num as New_IncExp_GL,
    interest_receivable_payable_gl_num as New_RecvPay_GL,
    interest_income_gl_num as Old_Income,
    interest_expenditure_gl_num as Old_Expend,
    interest_receivable_gl_num as Old_Recv,
    interest_payable_gl_num as Old_Pay
FROM sub_prod_master
ORDER BY Sub_Product_Id;

-- =====================================================
-- STEP 4: Drop old columns (execute after verification)
-- =====================================================
-- IMPORTANT: Only execute this after verifying data migration is correct!
-- Uncomment the lines below when ready:

-- ALTER TABLE sub_prod_master
-- DROP COLUMN interest_income_gl_num,
-- DROP COLUMN interest_expenditure_gl_num,
-- DROP COLUMN interest_receivable_gl_num,
-- DROP COLUMN interest_payable_gl_num;

-- =====================================================
-- STEP 5: Verify final structure
-- =====================================================

DESCRIBE sub_prod_master;

-- =====================================================
-- Expected Results After Migration:
--
-- Sub-Product 25 (TEST-111, LIABILITY - 110102001):
--   - interest_income_expenditure_gl_num: NULL or '140101001' (expenditure)
--   - interest_receivable_payable_gl_num: '130101000' (payable)
--
-- Sub-Product 26 (TEST-OD101, ASSET - 210201001):
--   - interest_income_expenditure_gl_num: NULL (no income configured)
--   - interest_receivable_payable_gl_num: NULL (no receivable configured)
--
-- Sub-Product 27 (SB-Sav-1, LIABILITY - 110101001):
--   - interest_income_expenditure_gl_num: NULL or configured (expenditure)
--   - interest_receivable_payable_gl_num: '130101001' (payable)
-- =====================================================
