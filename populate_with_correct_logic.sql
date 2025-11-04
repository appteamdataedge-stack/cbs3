-- Populate txn_hist_acct with correct balance calculation logic
-- Opening balance = Previous transaction's BALANCE_AFTER_TRAN
-- OR last day's closing balance from acct_bal
-- OR 0 for first transaction

USE moneymarketdb;

DELIMITER $$

CREATE PROCEDURE populate_txn_hist_correct()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_tran_id VARCHAR(20);
    DECLARE v_account_no VARCHAR(13);
    DECLARE v_tran_date DATE;
    DECLARE v_value_date DATE;
    DECLARE v_dr_cr_flag ENUM('D', 'C');
    DECLARE v_lcy_amt DECIMAL(20,2);
    DECLARE v_narration VARCHAR(100);
    DECLARE v_tran_ccy VARCHAR(3);
    DECLARE v_branch_id VARCHAR(10);
    DECLARE v_gl_num VARCHAR(9);
    DECLARE v_previous_balance DECIMAL(20,2);
    DECLARE v_balance_after DECIMAL(20,2);
    DECLARE v_tran_sl_no INT;
    
    -- Cursor for all verified transactions
    DECLARE cur CURSOR FOR 
        SELECT 
            t.Tran_Id,
            t.Account_No,
            t.Tran_Date,
            t.Value_Date,
            t.Dr_Cr_Flag,
            t.LCY_Amt,
            t.Narration,
            t.Tran_Ccy
        FROM tran_table t
        WHERE t.Tran_Status = 'Verified'
        ORDER BY t.Account_No ASC, t.Tran_Date ASC, t.Tran_Id ASC;
    
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    OPEN cur;
    
    read_loop: LOOP
        FETCH cur INTO v_tran_id, v_account_no, v_tran_date, v_value_date, 
                       v_dr_cr_flag, v_lcy_amt, v_narration, v_tran_ccy;
        
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        -- Get branch ID
        SELECT Branch_Code INTO v_branch_id
        FROM cust_acct_master
        WHERE Account_No = v_account_no
        LIMIT 1;
        
        IF v_branch_id IS NULL THEN
            SELECT Branch_Code INTO v_branch_id
            FROM of_acct_master
            WHERE Account_No = v_account_no
            LIMIT 1;
        END IF;
        
        IF v_branch_id IS NULL THEN
            SET v_branch_id = 'DEFAULT';
        END IF;
        
        -- Get GL number
        SELECT sp.Cum_GL_Num INTO v_gl_num
        FROM cust_acct_master ca
        JOIN sub_prod_master sp ON ca.Sub_Product_Id = sp.Sub_Product_Id
        WHERE ca.Account_No = v_account_no
        LIMIT 1;
        
        IF v_gl_num IS NULL THEN
            SELECT sp.Cum_GL_Num INTO v_gl_num
            FROM of_acct_master oa
            JOIN sub_prod_master sp ON oa.Sub_Product_Id = sp.Sub_Product_Id
            WHERE oa.Account_No = v_account_no
            LIMIT 1;
        END IF;
        
        -- Get previous balance for this account
        -- Logic: Get last transaction's BALANCE_AFTER_TRAN
        SELECT BALANCE_AFTER_TRAN INTO v_previous_balance
        FROM txn_hist_acct
        WHERE ACC_No = v_account_no
        ORDER BY TRAN_DATE DESC, RCRE_TIME DESC
        LIMIT 1;
        
        -- If no previous transaction, check acct_bal for last day's balance
        IF v_previous_balance IS NULL THEN
            SELECT Current_Balance INTO v_previous_balance
            FROM acct_bal
            WHERE Account_No = v_account_no
            AND Tran_Date < v_tran_date
            ORDER BY Tran_Date DESC
            LIMIT 1;
        END IF;
        
        -- If still null, default to 0 (first transaction for this account)
        IF v_previous_balance IS NULL THEN
            SET v_previous_balance = 0;
        END IF;
        
        -- Calculate balance after transaction
        -- BALANCE_AFTER_TRAN = Previous Balance +/- TRAN_AMT based on TRAN_TYPE
        IF v_dr_cr_flag = 'C' THEN
            SET v_balance_after = v_previous_balance + v_lcy_amt;
        ELSE
            SET v_balance_after = v_previous_balance - v_lcy_amt;
        END IF;
        
        -- Get serial number
        SELECT COALESCE(MAX(TRAN_SL_NO), 0) + 1 INTO v_tran_sl_no
        FROM txn_hist_acct
        WHERE TRAN_ID = v_tran_id;
        
        IF v_tran_sl_no IS NULL THEN
            SET v_tran_sl_no = 1;
        END IF;
        
        -- Insert into txn_hist_acct (without Opening_Balance)
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
        ) VALUES (
            v_branch_id,
            v_account_no,
            v_tran_id,
            v_tran_date,
            v_value_date,
            v_tran_sl_no,
            v_narration,
            v_dr_cr_flag,
            v_lcy_amt,
            v_balance_after,
            'SYSTEM',
            'MIGRATION',
            COALESCE(v_tran_ccy, 'BDT'),
            v_gl_num,
            CURDATE(),
            CURTIME()
        );
        
    END LOOP;
    
    CLOSE cur;
    
END$$

DELIMITER ;

-- Execute the procedure
SELECT 'Starting population with correct balance logic...' AS Status;
CALL populate_txn_hist_correct();
SELECT 'Population completed!' AS Status;

-- Drop the procedure
DROP PROCEDURE IF EXISTS populate_txn_hist_correct;

-- Show results
SELECT '========================================' AS Message;
SELECT 'Population Complete' AS Message;
SELECT '========================================' AS Message;

SELECT COUNT(*) AS 'Total Records Inserted' FROM txn_hist_acct;

SELECT 
    TRAN_TYPE,
    COUNT(*) AS 'Count',
    SUM(TRAN_AMT) AS 'Total Amount'
FROM txn_hist_acct
GROUP BY TRAN_TYPE;

SELECT '========================================' AS Message;
SELECT 'Sample Records with Balances' AS Message;
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
ORDER BY ACC_No, TRAN_DATE, RCRE_TIME
LIMIT 20;

