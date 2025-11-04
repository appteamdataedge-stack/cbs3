-- =====================================================
-- Simple Fix for Opening_Balance and BALANCE_AFTER_TRAN
-- Sequential calculation for accuracy
-- =====================================================

USE moneymarketdb;

DELIMITER $$

CREATE PROCEDURE calculate_balances()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_hist_id BIGINT;
    DECLARE v_acc_no VARCHAR(13);
    DECLARE v_tran_date DATE;
    DECLARE v_tran_type ENUM('D', 'C');
    DECLARE v_tran_amt DECIMAL(20,2);
    DECLARE v_opening_balance DECIMAL(20,2);
    DECLARE v_balance_after DECIMAL(20,2);
    DECLARE v_last_balance DECIMAL(20,2);
    DECLARE v_last_account VARCHAR(13);
    DECLARE v_last_date DATE;

    DECLARE cur CURSOR FOR
        SELECT Hist_ID, ACC_No, TRAN_DATE, TRAN_TYPE, TRAN_AMT
        FROM txn_hist_acct
        ORDER BY ACC_No, TRAN_DATE, RCRE_TIME, Hist_ID;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    SET v_last_balance = NULL;
    SET v_last_account = NULL;
    SET v_last_date = NULL;

    OPEN cur;

    read_loop: LOOP
        FETCH cur INTO v_hist_id, v_acc_no, v_tran_date, v_tran_type, v_tran_amt;

        IF done THEN
            LEAVE read_loop;
        END IF;

        -- Determine if this is FIRST transaction of the day or SUBSEQUENT
        IF v_last_account IS NULL OR
           v_last_account != v_acc_no OR
           v_last_date IS NULL OR
           v_last_date != v_tran_date THEN

            -- FIRST transaction of the day for this account
            -- Get opening balance from acct_bal
            SELECT COALESCE(Closing_Bal, Current_Balance, 0) INTO v_opening_balance
            FROM acct_bal
            WHERE Account_No = v_acc_no
            AND Tran_Date < v_tran_date
            ORDER BY Tran_Date DESC
            LIMIT 1;

            IF v_opening_balance IS NULL THEN
                SET v_opening_balance = 0;
            END IF;

        ELSE
            -- SUBSEQUENT transaction on SAME day for SAME account
            -- Opening balance = previous transaction's closing balance
            SET v_opening_balance = v_last_balance;
        END IF;

        -- Calculate balance after transaction
        IF v_tran_type = 'C' THEN
            SET v_balance_after = v_opening_balance + v_tran_amt;
        ELSE
            SET v_balance_after = v_opening_balance - v_tran_amt;
        END IF;

        -- Update the record
        UPDATE txn_hist_acct
        SET
            Opening_Balance = v_opening_balance,
            BALANCE_AFTER_TRAN = v_balance_after
        WHERE Hist_ID = v_hist_id;

        -- Store for next iteration
        SET v_last_balance = v_balance_after;
        SET v_last_account = v_acc_no;
        SET v_last_date = v_tran_date;

    END LOOP;

    CLOSE cur;

END$$

DELIMITER ;

-- Execute the procedure
SELECT 'Calculating balances...' AS Status;
CALL calculate_balances();
SELECT 'Balances calculated successfully!' AS Status;

-- Drop procedure
DROP PROCEDURE IF EXISTS calculate_balances;

-- Verification
SELECT '========================================' AS Message;
SELECT 'Verification Results' AS Message;
SELECT '========================================' AS Message;

SELECT COUNT(*) AS 'Total Records' FROM txn_hist_acct;

-- Show sample data
SELECT
    Hist_ID,
    ACC_No,
    TRAN_DATE,
    TRAN_TYPE,
    TRAN_AMT,
    Opening_Balance,
    BALANCE_AFTER_TRAN,
    CASE
        WHEN TRAN_TYPE = 'C' THEN TRAN_AMT
        WHEN TRAN_TYPE = 'D' THEN -TRAN_AMT
    END AS Expected_Change,
    BALANCE_AFTER_TRAN - Opening_Balance AS Actual_Change
FROM txn_hist_acct
ORDER BY ACC_No, TRAN_DATE, RCRE_TIME, Hist_ID
LIMIT 20;

-- Validate calculations
SELECT
    'Credit Transactions' AS Type,
    COUNT(*) AS Total,
    SUM(CASE WHEN ABS(BALANCE_AFTER_TRAN - (Opening_Balance + TRAN_AMT)) < 0.01 THEN 1 ELSE 0 END) AS Correct,
    SUM(CASE WHEN ABS(BALANCE_AFTER_TRAN - (Opening_Balance + TRAN_AMT)) >= 0.01 THEN 1 ELSE 0 END) AS Incorrect
FROM txn_hist_acct
WHERE TRAN_TYPE = 'C'
UNION ALL
SELECT
    'Debit Transactions' AS Type,
    COUNT(*) AS Total,
    SUM(CASE WHEN ABS(BALANCE_AFTER_TRAN - (Opening_Balance - TRAN_AMT)) < 0.01 THEN 1 ELSE 0 END) AS Correct,
    SUM(CASE WHEN ABS(BALANCE_AFTER_TRAN - (Opening_Balance - TRAN_AMT)) >= 0.01 THEN 1 ELSE 0 END) AS Incorrect
FROM txn_hist_acct
WHERE TRAN_TYPE = 'D';

-- Show balance progression for one account
SELECT '========================================' AS Message;
SELECT 'Balance Progression for Sample Account' AS Message;
SELECT '========================================' AS Message;

SELECT
    Hist_ID,
    TRAN_DATE,
    TRAN_TYPE,
    TRAN_AMT,
    Opening_Balance,
    BALANCE_AFTER_TRAN
FROM txn_hist_acct
WHERE ACC_No = (SELECT ACC_No FROM txn_hist_acct LIMIT 1)
ORDER BY TRAN_DATE, RCRE_TIME, Hist_ID;

SELECT '========================================' AS Message;
SELECT 'Balance calculation completed!' AS Message;
SELECT '========================================' AS Message;
