-- Final correct version with stored procedure
USE moneymarketdb;

DELIMITER $$

DROP PROCEDURE IF EXISTS populate_with_balance$$

CREATE PROCEDURE populate_with_balance()
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
    DECLARE v_current_account VARCHAR(13);
    
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
    
    -- Clear table
    TRUNCATE TABLE txn_hist_acct;
    SET FOREIGN_KEY_CHECKS = 0;
    
    OPEN cur;
    
    SET v_current_account = '';
    
    read_loop: LOOP
        FETCH cur INTO v_tran_id, v_account_no, v_tran_date, v_value_date, 
                       v_dr_cr_flag, v_lcy_amt, v_narration, v_tran_ccy;
        
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        -- New account - get starting balance
        IF v_account_no != v_current_account THEN
            SET v_current_account = v_account_no;
            
            -- Get balance BEFORE first transaction from acct_bal
            SELECT Current_Balance INTO v_previous_balance
            FROM acct_bal
            WHERE Account_No = v_account_no
            AND Tran_Date < v_tran_date
            ORDER BY Tran_Date DESC
            LIMIT 1;
            
            IF v_previous_balance IS NULL THEN
                SET v_previous_balance = 0;
            END IF;
        ELSE
            -- Same account - get last balance from txn_hist_acct
            SELECT BALANCE_AFTER_TRAN INTO v_previous_balance
            FROM txn_hist_acct
            WHERE ACC_No = v_account_no
            ORDER BY Hist_ID DESC
            LIMIT 1;
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
        
        -- Calculate balance
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
        
        -- Insert
        INSERT INTO txn_hist_acct (
            Branch_ID, ACC_No, TRAN_ID, TRAN_DATE, VALUE_DATE, TRAN_SL_NO,
            NARRATION, TRAN_TYPE, TRAN_AMT, BALANCE_AFTER_TRAN,
            ENTRY_USER_ID, AUTH_USER_ID, CURRENCY_CODE, GL_Num, RCRE_DATE, RCRE_TIME
        ) VALUES (
            v_branch_id, v_account_no, v_tran_id, v_tran_date, v_value_date, v_tran_sl_no,
            SUBSTRING(COALESCE(v_narration, ''), 1, 100), v_dr_cr_flag, v_lcy_amt, v_balance_after,
            'SYSTEM', 'MIGRATION', COALESCE(v_tran_ccy, 'BDT'), v_gl_num, CURDATE(), CURTIME()
        );
        
    END LOOP;
    
    CLOSE cur;
    SET FOREIGN_KEY_CHECKS = 1;
    
END$$

DELIMITER ;

-- Execute
SELECT 'Starting population...' AS Status;
CALL populate_with_balance();
DROP PROCEDURE IF EXISTS populate_with_balance;

-- Results
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

