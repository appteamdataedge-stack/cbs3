-- FINAL WORKING VERSION - Two-step approach
USE moneymarketdb;

-- Step 1: Insert all transactions with initial balance 0
TRUNCATE TABLE txn_hist_acct;
SET FOREIGN_KEY_CHECKS = 0;

INSERT INTO txn_hist_acct (
    Branch_ID, ACC_No, TRAN_ID, TRAN_DATE, VALUE_DATE, TRAN_SL_NO,
    NARRATION, TRAN_TYPE, TRAN_AMT, BALANCE_AFTER_TRAN,
    ENTRY_USER_ID, AUTH_USER_ID, CURRENCY_CODE, GL_Num, RCRE_DATE, RCRE_TIME
)
SELECT 
    COALESCE(
        (SELECT Branch_Code FROM cust_acct_master WHERE Account_No = t.Account_No LIMIT 1),
        (SELECT Branch_Code FROM of_acct_master WHERE Account_No = t.Account_No LIMIT 1),
        'DEFAULT'
    ) AS Branch_ID,
    t.Account_No AS ACC_No,
    t.Tran_Id AS TRAN_ID,
    t.Tran_Date AS TRAN_DATE,
    t.Value_Date AS VALUE_DATE,
    1 AS TRAN_SL_NO,
    SUBSTRING(COALESCE(t.Narration, ''), 1, 100) AS NARRATION,
    t.Dr_Cr_Flag AS TRAN_TYPE,
    t.LCY_Amt AS TRAN_AMT,
    0 AS BALANCE_AFTER_TRAN, -- Temp: will update in step 2
    'SYSTEM' AS ENTRY_USER_ID,
    'MIGRATION' AS AUTH_USER_ID,
    COALESCE(t.Tran_Ccy, 'BDT') AS CURRENCY_CODE,
    COALESCE(
        (SELECT sp.Cum_GL_Num FROM cust_acct_master ca JOIN sub_prod_master sp ON ca.Sub_Product_Id = sp.Sub_Product_Id WHERE ca.Account_No = t.Account_No LIMIT 1),
        (SELECT sp.Cum_GL_Num FROM of_acct_master oa JOIN sub_prod_master sp ON oa.Sub_Product_Id = sp.Sub_Product_Id WHERE oa.Account_No = t.Account_No LIMIT 1)
    ) AS GL_Num,
    CURDATE() AS RCRE_DATE,
    CURTIME() AS RCRE_TIME
FROM tran_table t
WHERE t.Tran_Status = 'Verified'
ORDER BY t.Account_No ASC, t.Tran_Date ASC, t.Tran_Id ASC;

SET FOREIGN_KEY_CHECKS = 1;

SELECT CONCAT('Step 1: Inserted ', ROW_COUNT(), ' records with temp balance') AS Status;

-- Step 2: Update balances with correct calculation using UPDATE with subquery
UPDATE txn_hist_acct AS hist1
JOIN (
    SELECT 
        h.Hist_ID,
        h.ACC_No,
        h.TRAN_DATE,
        h.TRAN_TYPE,
        h.TRAN_AMT,
        (
            SELECT IFNULL(ab.Current_Balance, 0)
            FROM acct_bal ab
            WHERE ab.Account_No = h.ACC_No
            AND ab.Tran_Date < h.TRAN_DATE
            ORDER BY ab.Tran_Date DESC
            LIMIT 1
        ) AS starting_balance,
        (
            SELECT COALESCE(SUM(
                CASE 
                    WHEN TRAN_TYPE = 'C' THEN TRAN_AMT
                    ELSE -TRAN_AMT
                END
            ), 0)
            FROM txn_hist_acct h2
            WHERE h2.ACC_No = h.ACC_No
            AND h2.Hist_ID < h.Hist_ID
        ) AS running_sum
    FROM txn_hist_acct h
) AS calc ON hist1.Hist_ID = calc.Hist_ID
SET hist1.BALANCE_AFTER_TRAN = calc.starting_balance + calc.running_sum +
    CASE WHEN calc.TRAN_TYPE = 'C' THEN calc.TRAN_AMT ELSE -calc.TRAN_AMT END;

SELECT 'Step 2: Updated all balances' AS Status;

-- Show results
SELECT '========================================' AS Msg;
SELECT CONCAT('Total: ', COUNT(*), ' records') AS Result FROM txn_hist_acct;

SELECT 
    TRAN_TYPE,
    COUNT(*) AS 'Count',
    SUM(TRAN_AMT) AS 'Total'
FROM txn_hist_acct
GROUP BY TRAN_TYPE;

SELECT '========================================' AS Msg;
SELECT 'Account 100000071001' AS Msg;
SELECT '========================================' AS Msg;

SELECT 
    TRAN_DATE,
    TRAN_TYPE,
    TRAN_AMT,
    BALANCE_AFTER_TRAN
FROM txn_hist_acct
WHERE ACC_No = '100000071001'
ORDER BY TRAN_DATE, RCRE_TIME;

