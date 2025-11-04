-- Update TXN_HIST_ACCT table structure
-- Remove Opening_Balance column as it's redundant

USE moneymarketdb;

-- Drop the table and recreate without Opening_Balance
DROP TABLE IF EXISTS txn_hist_acct;

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
    
    -- Balance Information (only BALANCE_AFTER_TRAN needed)
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

SELECT 'Table structure updated - Opening_Balance column removed' AS Status;

