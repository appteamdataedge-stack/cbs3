-- =====================================================
-- Populate TXN_HIST_ACCT with ALL Transactions
-- Description: Populates transaction history with ALL 
--              transactions regardless of status
-- =====================================================

USE moneymarketdb;

-- Clear existing data
TRUNCATE TABLE txn_hist_acct;

SELECT 'Populating with ALL transactions (not just Verified)...' AS Status;

-- Populate with ALL transactions
DELIMITER $$

CREATE PROCEDURE populate_all_txn_hist_acct()
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
    DECLARE v_tran_status VARCHAR(20);
    DECLARE v_branch_id VARCHAR(10);
    DECLARE v_gl_num VARCHAR(9);
    DECLARE v_opening_balance DECIMAL(20,2);
    DECLARE v_balance_after DECIMAL(20,2);
    DECLARE v_tran_sl_no INT;
    
    -- Cursor to fetch ALL transactions ordered by date and time
    DECLARE cur CURSOR FOR 
        SELECT 
            t.Tran_Id,
            t.Account_No,
            t.Tran_Date,
            t.Value_Date,
            t.Dr_Cr_Flag,
            t.LCY_Amt,
            t.Narration,
            t.Tran_Ccy,
            t.Tran_Status
        FROM tran_table t
        ORDER BY t.Tran_Date ASC, t.Tran_Id ASC;
    
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    -- Temporary table to track balances per account
    CREATE TEMPORARY TABLE IF NOT EXISTS temp_account_balances (
        account_no VARCHAR(13) PRIMARY KEY,
        current_balance DECIMAL(20,2) DEFAULT 0
    );
    
    OPEN cur;
    
    read_loop: LOOP
        FETCH cur INTO v_tran_id, v_account_no, v_tran_date, v_value_date, 
                       v_dr_cr_flag, v_lcy_amt, v_narration, v_tran_ccy, v_tran_status;
        
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        -- Get branch ID from account
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
        
        -- Get current balance
        SELECT current_balance INTO v_opening_balance
        FROM temp_account_balances
        WHERE account_no = v_account_no;
        
        IF v_opening_balance IS NULL THEN
            SELECT Current_Balance INTO v_opening_balance
            FROM acct_bal
            WHERE Account_No = v_account_no
            ORDER BY Tran_Date DESC
            LIMIT 1;
            
            IF v_opening_balance IS NULL THEN
                SET v_opening_balance = 0;
            END IF;
            
            INSERT INTO temp_account_balances (account_no, current_balance)
            VALUES (v_account_no, v_opening_balance);
        END IF;
        
        -- Calculate balance after transaction
        IF v_dr_cr_flag = 'C' THEN
            SET v_balance_after = v_opening_balance + v_lcy_amt;
        ELSE
            SET v_balance_after = v_opening_balance - v_lcy_amt;
        END IF;
        
        -- Get serial number
        SELECT COALESCE(MAX(TRAN_SL_NO), 0) + 1 INTO v_tran_sl_no
        FROM txn_hist_acct
        WHERE TRAN_ID = v_tran_id;
        
        IF v_tran_sl_no IS NULL THEN
            SET v_tran_sl_no = 1;
        END IF;
        
        -- Insert into txn_hist_acct
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
            v_opening_balance,
            v_balance_after,
            'SYSTEM',
            v_tran_status,
            COALESCE(v_tran_ccy, 'BDT'),
            v_gl_num,
            CURDATE(),
            CURTIME()
        );
        
        -- Update balance
        UPDATE temp_account_balances
        SET current_balance = v_balance_after
        WHERE account_no = v_account_no;
        
    END LOOP;
    
    CLOSE cur;
    DROP TEMPORARY TABLE IF EXISTS temp_account_balances;
    
END$$

DELIMITER ;

-- Execute
CALL populate_all_txn_hist_acct();
DROP PROCEDURE IF EXISTS populate_all_txn_hist_acct;

-- Show results
SELECT '========================================' AS Message;
SELECT 'Population Complete - ALL Transactions' AS Message;
SELECT '========================================' AS Message;

SELECT COUNT(*) AS 'Total Records Inserted' FROM txn_hist_acct;

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
    Opening_Balance,
    BALANCE_AFTER_TRAN,
    AUTH_USER_ID AS Status
FROM txn_hist_acct
ORDER BY TRAN_DATE DESC
LIMIT 20;

