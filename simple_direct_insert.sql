-- Simple direct insert without stored procedure
USE moneymarketdb;

-- Clear existing data
TRUNCATE TABLE txn_hist_acct;

-- Disable foreign key checks temporarily
SET FOREIGN_KEY_CHECKS = 0;

-- Direct insert with simple balance calculation
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
    BALANCE_AFTER_TRAN,
    ENTRY_USER_ID,
    AUTH_USER_ID,
    CURRENCY_CODE,
    GL_Num,
    RCRE_DATE,
    RCRE_TIME
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
    SUBSTRING(t.Narration, 1, 100) AS NARRATION,
    t.Dr_Cr_Flag AS TRAN_TYPE,
    t.LCY_Amt AS TRAN_AMT,
    CASE 
        WHEN t.Dr_Cr_Flag = 'C' THEN t.LCY_Amt
        ELSE -t.LCY_Amt
    END AS BALANCE_AFTER_TRAN,
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
ORDER BY t.Tran_Date ASC, t.Tran_Id ASC;

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;

-- Show results
SELECT '========================================' AS Message;
SELECT CONCAT('Inserted ', ROW_COUNT(), ' records') AS Result;
SELECT '========================================' AS Message;

SELECT COUNT(*) AS 'Total Records in txn_hist_acct' FROM txn_hist_acct;

SELECT 
    TRAN_TYPE,
    COUNT(*) AS 'Count',
    SUM(TRAN_AMT) AS 'Total Amount'
FROM txn_hist_acct
GROUP BY TRAN_TYPE;

SELECT '========================================' AS Message;
SELECT 'Sample Records' AS Message;
SELECT '========================================' AS Message;

SELECT 
    Hist_ID,
    ACC_No,
    TRAN_ID,
    TRAN_DATE,
    TRAN_TYPE,
    TRAN_AMT,
    BALANCE_AFTER_TRAN
FROM txn_hist_acct
ORDER BY TRAN_DATE, RCRE_TIME
LIMIT 20;

