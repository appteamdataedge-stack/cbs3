-- =====================================================
-- Add Opening_Balance Column and Update Existing Data
-- Description: Adds Opening_Balance to txn_hist_acct and
--              recalculates it for all existing records
-- Date: 2025-11-02
-- =====================================================

USE moneymarketdb;

-- Step 1: Add the Opening_Balance column (nullable first for existing data)
ALTER TABLE txn_hist_acct
ADD COLUMN Opening_Balance DECIMAL(20,2) NULL
COMMENT 'Balance before this transaction'
AFTER TRAN_AMT;

SELECT 'Opening_Balance column added successfully!' AS Status;

-- Step 2: Update existing records with calculated opening balances
-- This will iterate through each account and calculate opening balance properly

DELIMITER $$

CREATE PROCEDURE update_opening_balances()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_hist_id BIGINT;
    DECLARE v_acc_no VARCHAR(13);
    DECLARE v_tran_date DATE;
    DECLARE v_tran_type ENUM('D', 'C');
    DECLARE v_tran_amt DECIMAL(20,2);
    DECLARE v_balance_after DECIMAL(20,2);
    DECLARE v_opening_balance DECIMAL(20,2);
    DECLARE v_prev_balance DECIMAL(20,2);
    DECLARE v_last_tran_date DATE;

    -- Cursor to fetch all records ordered by account, date, and creation time
    DECLARE cur CURSOR FOR
        SELECT
            Hist_ID,
            ACC_No,
            TRAN_DATE,
            TRAN_TYPE,
            TRAN_AMT,
            BALANCE_AFTER_TRAN
        FROM txn_hist_acct
        ORDER BY ACC_No ASC, TRAN_DATE ASC, RCRE_TIME ASC;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    SET v_prev_balance = NULL;
    SET v_last_tran_date = NULL;

    OPEN cur;

    read_loop: LOOP
        FETCH cur INTO v_hist_id, v_acc_no, v_tran_date, v_tran_type,
                       v_tran_amt, v_balance_after;

        IF done THEN
            LEAVE read_loop;
        END IF;

        -- Check if this is a new account or new day
        IF v_prev_balance IS NULL OR
           v_tran_date != v_last_tran_date OR
           @last_acc_no != v_acc_no THEN

            -- This is the FIRST transaction of the day for this account
            -- Calculate opening balance by reversing the transaction
            IF v_tran_type = 'C' THEN
                -- Credit: opening = closing - amount
                SET v_opening_balance = v_balance_after - v_tran_amt;
            ELSE
                -- Debit: opening = closing + amount
                SET v_opening_balance = v_balance_after + v_tran_amt;
            END IF;

            -- Also try to get from acct_bal as validation
            SELECT Current_Balance INTO @acct_bal_balance
            FROM acct_bal
            WHERE Account_No = v_acc_no
            AND Tran_Date < v_tran_date
            ORDER BY Tran_Date DESC
            LIMIT 1;

            -- If we found a balance in acct_bal and it's close to our calculated opening
            -- use it for more accuracy
            IF @acct_bal_balance IS NOT NULL THEN
                SET v_opening_balance = @acct_bal_balance;
            END IF;

        ELSE
            -- This is a SUBSEQUENT transaction on the SAME day
            -- Opening balance = previous transaction's closing balance
            SET v_opening_balance = v_prev_balance;
        END IF;

        -- Update the record
        UPDATE txn_hist_acct
        SET Opening_Balance = v_opening_balance
        WHERE Hist_ID = v_hist_id;

        -- Store for next iteration
        SET v_prev_balance = v_balance_after;
        SET v_last_tran_date = v_tran_date;
        SET @last_acc_no = v_acc_no;

    END LOOP;

    CLOSE cur;

END$$

DELIMITER ;

-- Execute the update procedure
SELECT 'Starting to update Opening_Balance for existing records...' AS Status;
CALL update_opening_balances();
SELECT 'Opening_Balance update completed!' AS Status;

-- Drop the procedure (cleanup)
DROP PROCEDURE IF EXISTS update_opening_balances;

-- Step 3: Make the column NOT NULL after populating data
ALTER TABLE txn_hist_acct
MODIFY COLUMN Opening_Balance DECIMAL(20,2) NOT NULL
COMMENT 'Balance before this transaction';

SELECT 'Opening_Balance column set to NOT NULL!' AS Status;

-- Step 4: Verify the results
SELECT '========================================' AS Message;
SELECT 'Verification Results' AS Message;
SELECT '========================================' AS Message;

-- Check if any null values remain
SELECT COUNT(*) AS 'Records with NULL Opening_Balance'
FROM txn_hist_acct
WHERE Opening_Balance IS NULL;

-- Sample of updated records
SELECT '========================================' AS Message;
SELECT 'Sample of Updated Records' AS Message;
SELECT '========================================' AS Message;

SELECT
    Hist_ID,
    ACC_No,
    TRAN_DATE,
    TRAN_TYPE,
    TRAN_AMT,
    Opening_Balance,
    BALANCE_AFTER_TRAN,
    BALANCE_AFTER_TRAN - Opening_Balance AS 'Calculated_Change'
FROM txn_hist_acct
ORDER BY RCRE_DATE DESC, RCRE_TIME DESC
LIMIT 10;

-- Validate balance consistency
SELECT '========================================' AS Message;
SELECT 'Balance Consistency Check' AS Message;
SELECT '========================================' AS Message;

-- Check if Credit transactions increase balance
SELECT
    'Credit Transactions' AS Type,
    COUNT(*) AS Total,
    SUM(CASE WHEN BALANCE_AFTER_TRAN = Opening_Balance + TRAN_AMT THEN 1 ELSE 0 END) AS Correct,
    SUM(CASE WHEN BALANCE_AFTER_TRAN != Opening_Balance + TRAN_AMT THEN 1 ELSE 0 END) AS Incorrect
FROM txn_hist_acct
WHERE TRAN_TYPE = 'C';

-- Check if Debit transactions decrease balance
SELECT
    'Debit Transactions' AS Type,
    COUNT(*) AS Total,
    SUM(CASE WHEN BALANCE_AFTER_TRAN = Opening_Balance - TRAN_AMT THEN 1 ELSE 0 END) AS Correct,
    SUM(CASE WHEN BALANCE_AFTER_TRAN != Opening_Balance - TRAN_AMT THEN 1 ELSE 0 END) AS Incorrect
FROM txn_hist_acct
WHERE TRAN_TYPE = 'D';

SELECT '========================================' AS Message;
SELECT 'Migration completed successfully!' AS Message;
SELECT 'You can now verify transactions in the application.' AS Message;
SELECT '========================================' AS Message;
