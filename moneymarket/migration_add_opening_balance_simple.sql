-- =====================================================
-- SIMPLE MIGRATION: Clear and Recreate TXN_HIST_ACCT
-- Description: Drops and recreates the table with Opening_Balance
--              Use this if you don't need to preserve existing data
-- Date: 2025-11-02
-- =====================================================

USE moneymarketdb;

-- Step 1: Backup existing data (optional - comment out if not needed)
CREATE TABLE IF NOT EXISTS txn_hist_acct_backup_20251102 AS
SELECT * FROM txn_hist_acct;

SELECT CONCAT('Backed up ', COUNT(*), ' records') AS 'Backup Status'
FROM txn_hist_acct_backup_20251102;

-- Step 2: Drop the existing table
DROP TABLE IF EXISTS txn_hist_acct;

-- Step 3: Recreate table WITH Opening_Balance column
CREATE TABLE txn_hist_acct (
    -- Primary Key
    Hist_ID BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Unique history record identifier',

    -- Transaction Details
    Branch_ID VARCHAR(10) NOT NULL COMMENT 'Branch identifier',
    ACC_No VARCHAR(13) NOT NULL COMMENT 'Account number',
    TRAN_ID VARCHAR(20) NOT NULL COMMENT 'Foreign Key to tran_table(Tran_Id)',
    TRAN_DATE DATE NOT NULL COMMENT 'Transaction date',
    VALUE_DATE DATE NOT NULL COMMENT 'Value date',
    TRAN_SL_NO INT NOT NULL COMMENT 'Serial number for debit/credit leg',
    NARRATION VARCHAR(100) COMMENT 'Transaction description',
    TRAN_TYPE ENUM('D', 'C') NOT NULL COMMENT 'Debit or Credit',
    TRAN_AMT DECIMAL(20,2) NOT NULL COMMENT 'Transaction amount',

    -- Balance Information (NEW FIELD ADDED)
    Opening_Balance DECIMAL(20,2) NOT NULL COMMENT 'Balance before this transaction',
    BALANCE_AFTER_TRAN DECIMAL(20,2) NOT NULL COMMENT 'Running balance after transaction',

    -- User & System Info
    ENTRY_USER_ID VARCHAR(20) NOT NULL COMMENT 'User who posted transaction',
    AUTH_USER_ID VARCHAR(20) COMMENT 'User who verified/authorized transaction',
    CURRENCY_CODE VARCHAR(3) DEFAULT 'BDT' COMMENT 'Transaction currency',
    GL_Num VARCHAR(9) COMMENT 'GL number from account sub-product',
    RCRE_DATE DATE NOT NULL COMMENT 'Record creation date',
    RCRE_TIME TIME NOT NULL COMMENT 'Record creation time',

    -- Indexes for performance
    INDEX idx_acc_no (ACC_No),
    INDEX idx_tran_date (TRAN_DATE),
    INDEX idx_acc_tran_date (ACC_No, TRAN_DATE),
    INDEX idx_tran_id (TRAN_ID),

    -- Foreign Key Constraint
    CONSTRAINT fk_txn_hist_tran_id
        FOREIGN KEY (TRAN_ID)
        REFERENCES tran_table(Tran_Id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Transaction history for Statement of Accounts';

SELECT 'Table txn_hist_acct recreated successfully with Opening_Balance column!' AS Status;

-- Step 4: Repopulate from tran_table (only VERIFIED transactions)
DELIMITER $$

CREATE PROCEDURE repopulate_txn_hist_acct()
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
    DECLARE v_opening_balance DECIMAL(20,2);
    DECLARE v_balance_after DECIMAL(20,2);
    DECLARE v_tran_sl_no INT;
    DECLARE v_last_balance DECIMAL(20,2);
    DECLARE v_last_date DATE;
    DECLARE v_last_account VARCHAR(13);

    -- Cursor to fetch all verified transactions ordered by date
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

    -- Initialize tracking variables
    SET v_last_balance = NULL;
    SET v_last_date = NULL;
    SET v_last_account = NULL;

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

        -- Determine opening balance (FIRST vs SUBSEQUENT transaction logic)
        IF v_last_account IS NULL OR v_last_account != v_account_no OR
           v_last_date IS NULL OR v_last_date != v_tran_date THEN
            -- FIRST transaction of the day for this account
            -- Get from acct_bal
            SELECT Current_Balance INTO v_opening_balance
            FROM acct_bal
            WHERE Account_No = v_account_no
            AND Tran_Date < v_tran_date
            ORDER BY Tran_Date DESC
            LIMIT 1;

            IF v_opening_balance IS NULL THEN
                -- Try closing balance
                SELECT Closing_Bal INTO v_opening_balance
                FROM acct_bal
                WHERE Account_No = v_account_no
                ORDER BY Tran_Date DESC
                LIMIT 1;
            END IF;

            IF v_opening_balance IS NULL THEN
                SET v_opening_balance = 0;
            END IF;
        ELSE
            -- SUBSEQUENT transaction on SAME day for SAME account
            -- Use previous transaction's closing balance
            SET v_opening_balance = v_last_balance;
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

        -- Insert record with Opening_Balance
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
            'MIGRATION',
            COALESCE(v_tran_ccy, 'BDT'),
            v_gl_num,
            CURDATE(),
            CURTIME()
        );

        -- Update tracking variables
        SET v_last_balance = v_balance_after;
        SET v_last_date = v_tran_date;
        SET v_last_account = v_account_no;

    END LOOP;

    CLOSE cur;

END$$

DELIMITER ;

-- Execute the population procedure
SELECT 'Starting to repopulate txn_hist_acct with Opening_Balance...' AS Status;
CALL repopulate_txn_hist_acct();
SELECT 'Repopulation completed!' AS Status;

-- Drop the procedure (cleanup)
DROP PROCEDURE IF EXISTS repopulate_txn_hist_acct;

-- Step 5: Verify the results
SELECT '========================================' AS Message;
SELECT 'Verification Results' AS Message;
SELECT '========================================' AS Message;

-- Count of records
SELECT COUNT(*) AS 'Total Records' FROM txn_hist_acct;

-- Sample records with Opening_Balance
SELECT
    Hist_ID,
    ACC_No,
    TRAN_DATE,
    TRAN_TYPE,
    TRAN_AMT,
    Opening_Balance,
    BALANCE_AFTER_TRAN,
    (BALANCE_AFTER_TRAN - Opening_Balance) AS 'Net_Change'
FROM txn_hist_acct
ORDER BY RCRE_DATE DESC, RCRE_TIME DESC
LIMIT 15;

SELECT '========================================' AS Message;
SELECT 'Migration completed successfully!' AS Message;
SELECT 'The application should now work correctly.' AS Message;
SELECT '========================================' AS Message;
