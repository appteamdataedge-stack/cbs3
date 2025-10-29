-- V6: Remove CURRENT_TIMESTAMP defaults for CBS System Date Compliance
-- This migration removes all database-level timestamp defaults to ensure
-- all timestamps are controlled by the application's SystemDateService

-- Remove CURRENT_TIMESTAMP defaults from Acct_Bal table
ALTER TABLE Acct_Bal 
MODIFY COLUMN Last_Updated TIMESTAMP NOT NULL;

-- Remove CURRENT_TIMESTAMP defaults from GL_Balance table  
ALTER TABLE GL_Balance
MODIFY COLUMN Last_Updated TIMESTAMP NOT NULL;

-- Remove CURRENT_TIMESTAMP defaults from Account_Seq table
ALTER TABLE Account_Seq
MODIFY COLUMN Last_Updated TIMESTAMP NOT NULL;

-- Remove CURRENT_TIMESTAMP defaults from Parameter_Table
ALTER TABLE Parameter_Table
MODIFY COLUMN Last_Updated TIMESTAMP NOT NULL;

-- Remove CURRENT_TIMESTAMP defaults from EOD_Log_Table
ALTER TABLE EOD_Log_Table
MODIFY COLUMN Created_Timestamp TIMESTAMP NOT NULL;

-- Update existing records to use a default timestamp if they have NULL values
-- This ensures data integrity during the migration
UPDATE Acct_Bal SET Last_Updated = '2024-01-01 00:00:00' WHERE Last_Updated IS NULL;
UPDATE GL_Balance SET Last_Updated = '2024-01-01 00:00:00' WHERE Last_Updated IS NULL;
UPDATE Account_Seq SET Last_Updated = '2024-01-01 00:00:00' WHERE Last_Updated IS NULL;
UPDATE Parameter_Table SET Last_Updated = '2024-01-01 00:00:00' WHERE Last_Updated IS NULL;
UPDATE EOD_Log_Table SET Created_Timestamp = '2024-01-01 00:00:00' WHERE Created_Timestamp IS NULL;

-- Add comments to document the CBS compliance change
ALTER TABLE Acct_Bal COMMENT = 'Account balances - CBS Compliance: Last_Updated controlled by SystemDateService';
ALTER TABLE GL_Balance COMMENT = 'GL balances - CBS Compliance: Last_Updated controlled by SystemDateService';
ALTER TABLE Account_Seq COMMENT = 'Account sequences - CBS Compliance: Last_Updated controlled by SystemDateService';
ALTER TABLE Parameter_Table COMMENT = 'System parameters - CBS Compliance: Last_Updated controlled by SystemDateService';
ALTER TABLE EOD_Log_Table COMMENT = 'EOD processing logs - CBS Compliance: Created_Timestamp controlled by SystemDateService';
