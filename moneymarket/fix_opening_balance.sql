-- =====================================================
-- Fix Opening_Balance for Existing Records
-- Direct calculation based on BALANCE_AFTER_TRAN
-- =====================================================

USE moneymarketdb;

-- Step 1: Update opening balance by reversing the calculation
-- For Credit: Opening = Closing - Amount
-- For Debit:  Opening = Closing + Amount

UPDATE txn_hist_acct
SET Opening_Balance = CASE
    WHEN TRAN_TYPE = 'C' THEN BALANCE_AFTER_TRAN - TRAN_AMT
    WHEN TRAN_TYPE = 'D' THEN BALANCE_AFTER_TRAN + TRAN_AMT
    ELSE 0
END
WHERE Opening_Balance IS NULL;

SELECT CONCAT('Updated ', ROW_COUNT(), ' records with calculated opening balance') AS Status;

-- Step 2: Now refine by using proper sequence for same-day transactions
-- Create temporary table to track proper opening balances
CREATE TEMPORARY TABLE temp_proper_balances AS
SELECT
    h1.Hist_ID,
    h1.ACC_No,
    h1.TRAN_DATE,
    h1.TRAN_TYPE,
    h1.TRAN_AMT,
    h1.BALANCE_AFTER_TRAN,
    COALESCE(
        -- Get previous transaction's closing balance (same account, same date)
        (SELECT h2.BALANCE_AFTER_TRAN
         FROM txn_hist_acct h2
         WHERE h2.ACC_No = h1.ACC_No
         AND h2.TRAN_DATE = h1.TRAN_DATE
         AND (h2.RCRE_TIME < h1.RCRE_TIME OR (h2.RCRE_TIME = h1.RCRE_TIME AND h2.Hist_ID < h1.Hist_ID))
         ORDER BY h2.RCRE_TIME DESC, h2.Hist_ID DESC
         LIMIT 1),
        -- If no previous transaction today, get from acct_bal
        (SELECT COALESCE(ab.Closing_Bal, ab.Current_Balance, 0)
         FROM acct_bal ab
         WHERE ab.Account_No = h1.ACC_No
         AND ab.Tran_Date < h1.TRAN_DATE
         ORDER BY ab.Tran_Date DESC
         LIMIT 1),
        -- Default to calculated value
        CASE
            WHEN h1.TRAN_TYPE = 'C' THEN h1.BALANCE_AFTER_TRAN - h1.TRAN_AMT
            WHEN h1.TRAN_TYPE = 'D' THEN h1.BALANCE_AFTER_TRAN + h1.TRAN_AMT
            ELSE 0
        END
    ) AS Proper_Opening_Balance
FROM txn_hist_acct h1
ORDER BY h1.ACC_No, h1.TRAN_DATE, h1.RCRE_TIME, h1.Hist_ID;

-- Step 3: Update with proper opening balances
UPDATE txn_hist_acct t
JOIN temp_proper_balances tp ON t.Hist_ID = tp.Hist_ID
SET t.Opening_Balance = tp.Proper_Opening_Balance;

SELECT CONCAT('Refined opening balances for all records') AS Status;

-- Step 4: Make column NOT NULL
ALTER TABLE txn_hist_acct
MODIFY COLUMN Opening_Balance DECIMAL(20,2) NOT NULL
COMMENT 'Balance before this transaction';

SELECT 'Opening_Balance column set to NOT NULL!' AS Status;

-- Step 5: Verification
SELECT '========================================' AS Message;
SELECT 'Verification Results' AS Message;
SELECT '========================================' AS Message;

-- Check for NULL values
SELECT COUNT(*) AS 'Records with NULL Opening_Balance'
FROM txn_hist_acct
WHERE Opening_Balance IS NULL;

-- Sample of records
SELECT
    Hist_ID,
    ACC_No,
    TRAN_DATE,
    TRAN_TYPE,
    TRAN_AMT,
    Opening_Balance,
    BALANCE_AFTER_TRAN,
    CASE
        WHEN TRAN_TYPE = 'C' THEN TRAN_AMT
        WHEN TRAN_TYPE = 'D' THEN -TRAN_AMT
    END AS Expected_Change,
    BALANCE_AFTER_TRAN - Opening_Balance AS Actual_Change
FROM txn_hist_acct
ORDER BY TRAN_DATE DESC, RCRE_TIME DESC
LIMIT 10;

-- Validate calculations
SELECT
    'Credit Transactions' AS Type,
    COUNT(*) AS Total,
    SUM(CASE WHEN ABS(BALANCE_AFTER_TRAN - (Opening_Balance + TRAN_AMT)) < 0.01 THEN 1 ELSE 0 END) AS Correct,
    SUM(CASE WHEN ABS(BALANCE_AFTER_TRAN - (Opening_Balance + TRAN_AMT)) >= 0.01 THEN 1 ELSE 0 END) AS Incorrect
FROM txn_hist_acct
WHERE TRAN_TYPE = 'C'
UNION ALL
SELECT
    'Debit Transactions' AS Type,
    COUNT(*) AS Total,
    SUM(CASE WHEN ABS(BALANCE_AFTER_TRAN - (Opening_Balance - TRAN_AMT)) < 0.01 THEN 1 ELSE 0 END) AS Correct,
    SUM(CASE WHEN ABS(BALANCE_AFTER_TRAN - (Opening_Balance - TRAN_AMT)) >= 0.01 THEN 1 ELSE 0 END) AS Incorrect
FROM txn_hist_acct
WHERE TRAN_TYPE = 'D';

SELECT '========================================' AS Message;
SELECT 'Fix completed successfully!' AS Message;
SELECT '========================================' AS Message;
