-- Add Parameter_Table for system configuration
CREATE TABLE Parameter_Table (
  Parameter_Id INT AUTO_INCREMENT PRIMARY KEY,
  Parameter_Name VARCHAR(50) NOT NULL UNIQUE,
  Parameter_Value VARCHAR(100) NOT NULL,
  Parameter_Description VARCHAR(200),
  Last_Updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  Updated_By VARCHAR(20) NOT NULL
);

-- Add EOD_Log_Table for tracking EOD job executions
CREATE TABLE EOD_Log_Table (
  EOD_Log_Id BIGINT AUTO_INCREMENT PRIMARY KEY,
  EOD_Date DATE NOT NULL,
  Job_Name VARCHAR(50) NOT NULL,
  Start_Timestamp TIMESTAMP NOT NULL,
  End_Timestamp TIMESTAMP NULL,
  System_Date DATE NOT NULL,
  User_ID VARCHAR(20) NOT NULL,
  Records_Processed INT DEFAULT 0,
  Status ENUM('Running', 'Success', 'Failed') NOT NULL DEFAULT 'Running',
  Error_Message TEXT,
  Failed_At_Step VARCHAR(100),
  Created_Timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add indexes for performance
CREATE INDEX idx_eod_log_date ON EOD_Log_Table(EOD_Date);
CREATE INDEX idx_eod_log_job_name ON EOD_Log_Table(Job_Name);
CREATE INDEX idx_eod_log_status ON EOD_Log_Table(Status);
CREATE INDEX idx_tran_table_date ON Tran_Table(Tran_Date);
CREATE INDEX idx_tran_table_status ON Tran_Table(Tran_Status);
CREATE INDEX idx_acct_bal_date ON Acct_Bal(Tran_Date);
CREATE INDEX idx_gl_movement_date ON GL_Movement(Tran_Date);

-- Insert default system parameters
INSERT INTO Parameter_Table (Parameter_Name, Parameter_Value, Parameter_Description, Updated_By) VALUES
('System_Date', CURDATE(), 'Current system date for EOD processing', 'SYSTEM'),
('Last_EOD_Date', NULL, 'Date of last successful EOD run', 'SYSTEM'),
('Last_EOD_Timestamp', NULL, 'Timestamp of last successful EOD run', 'SYSTEM'),
('Last_EOD_User', NULL, 'User who ran the last successful EOD', 'SYSTEM'),
('EOD_Admin_User', 'ADMIN', 'User ID authorized to run EOD operations', 'SYSTEM'),
('Interest_Default_Divisor', '36500', 'Default divisor for interest calculations', 'SYSTEM'),
('Currency_Default', 'BDT', 'Default currency code', 'SYSTEM'),
('Exchange_Rate_Default', '1.0', 'Default exchange rate', 'SYSTEM');
