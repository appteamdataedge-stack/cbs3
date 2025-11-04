USE moneymarketdb;

DELIMITER $$

DROP PROCEDURE IF EXISTS test_insert$$

CREATE PROCEDURE test_insert()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_tran_id VARCHAR(20);
    DECLARE v_account_no VARCHAR(13);
    DECLARE v_count INT DEFAULT 0;
    
    DECLARE cur CURSOR FOR 
        SELECT t.Tran_Id, t.Account_No
        FROM tran_table t
        WHERE t.Tran_Status = 'Verified'
        ORDER BY t.Account_No ASC, t.Tran_Date ASC, t.Tran_Id ASC
        LIMIT 10;
    
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    SET FOREIGN_KEY_CHECKS = 0;
    
    OPEN cur;
    
    read_loop: LOOP
        FETCH cur INTO v_tran_id, v_account_no;
        
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        SET v_count = v_count + 1;
        
        SELECT CONCAT('Processing #', v_count, ': ', v_tran_id) AS Info;
        
        -- Try to insert
        INSERT INTO txn_hist_acct (
            Branch_ID, ACC_No, TRAN_ID, TRAN_DATE, VALUE_DATE, TRAN_SL_NO,
            NARRATION, TRAN_TYPE, TRAN_AMT, BALANCE_AFTER_TRAN,
            ENTRY_USER_ID, AUTH_USER_ID, CURRENCY_CODE, GL_Num, RCRE_DATE, RCRE_TIME
        ) VALUES (
            '001', v_account_no, v_tran_id, '2025-01-01', '2025-01-01', 1,
            'Test', 'C', 100, 100,
            'SYSTEM', 'TEST', 'BDT', '123456789', CURDATE(), CURTIME()
        );
        
    END LOOP;
    
    CLOSE cur;
    SET FOREIGN_KEY_CHECKS = 1;
    
    SELECT CONCAT('Total inserted: ', v_count) AS Result;
    
END$$

DELIMITER ;

TRUNCATE TABLE txn_hist_acct;
CALL test_insert();
SELECT COUNT(*) AS 'Records in table' FROM txn_hist_acct;
DROP PROCEDURE IF EXISTS test_insert;

