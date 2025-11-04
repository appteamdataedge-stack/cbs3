-- Final working version with correct balance calculation
USE moneymarketdb;

-- Clear table
TRUNCATE TABLE txn_hist_acct;
SET FOREIGN_KEY_CHECKS = 0;

-- Use a simple approach: Insert with variables to track balances
SET @prev_account = '';
SET @prev_balance = 0;

INSERT INTO txn_hist_acct (
    Branch_ID, ACC_No, TRAN_ID, TRAN_DATE, VALUE_DATE, TRAN_SL_NO,
    NARRATION, TRAN_TYPE, TRAN_AMT, BALANCE_AFTER_TRAN,
    ENTRY_USER_ID, AUTH_USER_ID, CURRENCY_CODE, GL_Num, RCRE_DATE, RCRE_TIME
)
SELECT 
    -- Branch ID
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
    
    -- Calculate BALANCE_AFTER_TRAN
    @prev_balance := CASE 
        WHEN @prev_account != t.Account_No THEN
            -- New account, get balance from acct_bal
            IFNULL((SELECT Current_Balance FROM acct_bal WHERE Account_No = t.Account_No ORDER BY Tran_Date DESC LIMIT 1), 0) +
            CASE WHEN t.Dr_Cr_Flag = 'C' THEN t.LCY_Amt ELSE -t.LCY_Amt END
        ELSE
            -- Same account, use previous balance
            @prev_balance +
            CASE WHEN t.Dr_Cr_Flag = 'C' THEN t.LCY_Amt ELSE -t.LCY_Amt END
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
ORDER BY t.Account_No ASC, t.Tran_Date ASC, t.Tran_Id ASC;

SET FOREIGN_KEY_CHECKS = 1;

-- Show results
SELECT '========================================' AS Message;
SELECT CONCAT('Inserted ', ROW_COUNT(), ' records') AS Result;
SELECT '========================================' AS Message;

SELECT COUNT(*) AS 'Total Records' FROM txn_hist_acct;

-- Show sample for your test account
SELECT '========================================' AS Message;
SELECT 'Account 100000071001 Transactions' AS Message;
SELECT '========================================' AS Message;

SELECT 
    Hist_ID,
    TRAN_DATE,
    TRAN_TYPE,
    TRAN_AMT,
    BALANCE_AFTER_TRAN
FROM txn_hist_acct
WHERE ACC_No = '100000071001'
ORDER BY TRAN_DATE, RCRE_TIME;

