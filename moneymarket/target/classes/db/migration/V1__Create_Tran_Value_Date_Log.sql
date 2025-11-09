-- Create table for logging value-dated transactions
-- This table tracks delta interest calculations and posting status for past/future-dated transactions

CREATE TABLE IF NOT EXISTS Tran_Value_Date_Log (
    Log_Id BIGINT AUTO_INCREMENT PRIMARY KEY,
    Tran_Id VARCHAR(20) NOT NULL,
    Value_Date DATE NOT NULL,
    Days_Difference INT NOT NULL COMMENT 'Positive for past-dated, negative for future-dated',
    Delta_Interest_Amt DECIMAL(20, 4) DEFAULT 0.0000 COMMENT 'Calculated delta interest amount',
    Adjustment_Posted_Flag VARCHAR(1) DEFAULT 'N' COMMENT 'Y=Posted, N=Pending',
    Created_Timestamp DATETIME NOT NULL,

    INDEX idx_tran_id (Tran_Id),
    INDEX idx_value_date (Value_Date),
    INDEX idx_posted_flag (Adjustment_Posted_Flag),
    INDEX idx_value_date_flag (Value_Date, Adjustment_Posted_Flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Logs value-dated transactions and interest adjustments';
