USE moneymarketdb;

DELIMITER $$

DROP PROCEDURE IF EXISTS debug_populate$$

CREATE PROCEDURE debug_populate()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_count INT DEFAULT 0;
    DECLARE v_tran_id VARCHAR(20);
    DECLARE v_account_no VARCHAR(13);
    
    DECLARE cur CURSOR FOR 
        SELECT t.Tran_Id, t.Account_No
        FROM tran_table t
        WHERE t.Tran_Status = 'Verified'
        ORDER BY t.Account_No ASC, t.Tran_Date ASC, t.Tran_Id ASC;
    
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    SELECT 'Starting...' AS Info;
    
    OPEN cur;
    
    read_loop: LOOP
        FETCH cur INTO v_tran_id, v_account_no;
        
        IF done THEN
            SELECT CONCAT('Done. Processed: ', v_count) AS Info;
            LEAVE read_loop;
        END IF;
        
        SET v_count = v_count + 1;
        
        IF v_count <= 5 THEN
            SELECT CONCAT('Processing #', v_count, ': ', v_tran_id, ' for ', v_account_no) AS Info;
        END IF;
        
    END LOOP;
    
    CLOSE cur;
    
END$$

DELIMITER ;

CALL debug_populate();
DROP PROCEDURE IF EXISTS debug_populate;

