-- Ultimate simple version - just insert without complex logic for now
USE moneymarketdb;

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
    ),
    t.Account_No,
    t.Tran_Id,
    t.Tran_Date,
    t.Value_Date,
    1,
    SUBSTRING(COALESCE(t.Narration, ''), 1, 100),
    t.Dr_Cr_Flag,
    t.LCY_Amt,
    0, -- Temp: set to 0 for now
    'SYSTEM',
    'MIGRATION',
    COALESCE(t.Tran_Ccy, 'BDT'),
    COALESCE(
        (SELECT sp.Cum_GL_Num FROM cust_acct_master ca JOIN sub_prod_master sp ON ca.Sub_Product_Id = sp.Sub_Product_Id WHERE ca.Account_No = t.Account_No LIMIT 1),
        (SELECT sp.Cum_GL_Num FROM of_acct_master oa JOIN sub_prod_master sp ON oa.Sub_Product_Id = sp.Sub_Product_Id WHERE oa.Account_No = t.Account_No LIMIT 1)
    ),
    CURDATE(),
    CURTIME()
FROM tran_table t
WHERE t.Tran_Status = 'Verified'
ORDER BY t.Account_No, t.Tran_Date, t.Tran_Id;

SET FOREIGN_KEY_CHECKS = 1;

SELECT CONCAT('Inserted ', COUNT(*), ' records') AS Result FROM txn_hist_acct;

