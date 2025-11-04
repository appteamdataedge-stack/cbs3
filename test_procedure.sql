USE moneymarketdb;

-- Test the procedure with error display
DELIMITER $$

DROP PROCEDURE IF EXISTS populate_with_balance$$

CREATE PROCEDURE populate_with_balance()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_tran_id VARCHAR(20);
    DECLARE v_account_no VARCHAR(13);
    DECLARE v_previous_balance DECIMAL(20,2);
    
    DECLARE cur CURSOR FOR 
        SELECT t.Tran_Id, t.Account_No
        FROM tran_table t
        WHERE t.Tran_Status = 'Verified' AND t.Account_No = '100000071001'
        ORDER BY t.Tran_Date ASC, t.Tran_Id ASC
        LIMIT 3;
    
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    OPEN cur;
    
    read_loop: LOOP
        FETCH cur INTO v_tran_id, v_account_no;
        
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        SELECT CONCAT('Processing: ', v_tran_id, ' for account: ', v_account_no) AS Info;
        
        -- Get balance
        SELECT Current_Balance INTO v_previous_balance
        FROM acct_bal
        WHERE Account_No = v_account_no
        ORDER BY Tran_Date DESC
        LIMIT 1;
        
        SELECT CONCAT('Previous balance: ', v_previous_balance) AS Info;
        
    END LOOP;
    
    CLOSE cur;
    
END$$

DELIMITER ;

CALL populate_with_balance();
DROP PROCEDURE IF EXISTS populate_with_balance;

