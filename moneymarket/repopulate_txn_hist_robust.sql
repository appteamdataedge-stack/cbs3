-- =====================================================
-- Robust Repopulation of txn_hist_acct
-- Handles missing accounts gracefully
-- =====================================================

USE moneymarketdb;

-- Clear existing records
TRUNCATE TABLE txn_hist_acct;

SELECT 'Starting robust repopulation...' AS Status;

-- Insert all verified transactions with calculated opening/closing balances
INSERT INTO txn_hist_acct (
    Branch_ID,
    ACC_No,
    TRAN_ID,
    TRAN_DATE,
    VALUE_DATE,
    TRAN_SL_NO,
    NARRATION,
    TRAN_TYPE,
    TRAN_AMT,
    Opening_Balance,
    BALANCE_AFTER_TRAN,
    ENTRY_USER_ID,
    AUTH_USER_ID,
    CURRENCY_CODE,
    GL_Num,
    RCRE_DATE,
    RCRE_TIME
)
SELECT
    -- Branch ID (with fallback)
    COALESCE(ca.Branch_Code, oa.Branch_Code, 'DEFAULT') AS Branch_ID,

    -- Account details
    t.Account_No AS ACC_No,
    t.Tran_Id AS TRAN_ID,
    t.Tran_Date AS TRAN_DATE,
    t.Value_Date AS VALUE_DATE,

    -- Serial number (simple counter for now)
    ROW_NUMBER() OVER (PARTITION BY t.Tran_Id ORDER BY t.Tran_Id) AS TRAN_SL_NO,

    t.Narration AS NARRATION,
    t.Dr_Cr_Flag AS TRAN_TYPE,
    t.LCY_Amt AS TRAN_AMT,

    -- Opening Balance (will be updated in next step for sequential accuracy)
    0.00 AS Opening_Balance,

    -- Balance After Transaction (will be calculated in next step)
    0.00 AS BALANCE_AFTER_TRAN,

    -- User info
    COALESCE(t.UDF1, 'SYSTEM') AS ENTRY_USER_ID,
    'MIGRATION' AS AUTH_USER_ID,
    COALESCE(t.Tran_Ccy, 'BDT') AS CURRENCY_CODE,

    -- GL Number (with fallback)
    COALESCE(sp_cust.Cum_GL_Num, sp_office.Cum_GL_Num) AS GL_Num,

    -- System dates
    CURDATE() AS RCRE_DATE,
    CURTIME() AS RCRE_TIME

FROM tran_table t
LEFT JOIN cust_acct_master ca ON t.Account_No = ca.Account_No
LEFT JOIN of_acct_master oa ON t.Account_No = oa.Account_No
LEFT JOIN sub_prod_master sp_cust ON ca.Sub_Product_Id = sp_cust.Sub_Product_Id
LEFT JOIN sub_prod_master sp_office ON oa.Sub_Product_Id = sp_office.Sub_Product_Id
WHERE t.Tran_Status = 'Verified'
ORDER BY t.Account_No, t.Tran_Date, t.Tran_Id;

SELECT CONCAT('Inserted ', ROW_COUNT(), ' transaction records') AS Status;

-- Now update balances using a more robust approach
-- Set variables for cursor
SET @last_account = '';
SET @last_date = NULL;
SET @running_balance = 0;

-- Update balances in order
UPDATE txn_hist_acct t1
JOIN (
    SELECT
        Hist_ID,
        ACC_No,
        TRAN_DATE,
        TRAN_TYPE,
        TRAN_AMT,
        -- Calculate opening balance
        COALESCE(
            LAG(TRAN_AMT_SIGNED, 1) OVER w_running_balance,
            (SELECT COALESCE(ab.Closing_Bal, ab.Current_Balance, 0)
             FROM acct_bal ab
             WHERE ab.Account_No = txn_hist_acct.ACC_No
             AND ab.Tran_Date < txn_hist_acct.TRAN_DATE
             ORDER BY ab.Tran_Date DESC
             LIMIT 1),
            0
        ) AS Calc_Opening_Balance,
        -- Calculate balance after
        SUM(TRAN_AMT_SIGNED) OVER w_running_balance AS Calc_Balance_After
    FROM (
        SELECT
            Hist_ID,
            ACC_No,
            TRAN_DATE,
            TRAN_TYPE,
            TRAN_AMT,
            CASE
                WHEN TRAN_TYPE = 'C' THEN TRAN_AMT
                WHEN TRAN_TYPE = 'D' THEN -TRAN_AMT
                ELSE 0
            END AS TRAN_AMT_SIGNED
        FROM txn_hist_acct
    ) sub
    WINDOW w_running_balance AS (
        PARTITION BY ACC_No
        ORDER BY TRAN_DATE, RCRE_TIME, Hist_ID
        ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
    )
) t2 ON t1.Hist_ID = t2.Hist_ID
SET
    t1.Opening_Balance = t2.Calc_Opening_Balance,
    t1.BALANCE_AFTER_TRAN = t2.Calc_Balance_After;

SELECT 'Balances calculated and updated!' AS Status;

-- Verification
SELECT '========================================' AS Message;
SELECT 'Verification Results' AS Message;
SELECT '========================================' AS Message;

SELECT COUNT(*) AS 'Total Records' FROM txn_hist_acct;

SELECT
    Hist_ID,
    ACC_No,
    TRAN_DATE,
    TRAN_TYPE,
    TRAN_AMT,
    Opening_Balance,
    BALANCE_AFTER_TRAN,
    BALANCE_AFTER_TRAN - Opening_Balance AS Net_Change
FROM txn_hist_acct
ORDER BY TRAN_DATE DESC, RCRE_TIME DESC
LIMIT 15;

SELECT '========================================' AS Message;
SELECT 'Repopulation completed!' AS Message;
SELECT '========================================' AS Message;
