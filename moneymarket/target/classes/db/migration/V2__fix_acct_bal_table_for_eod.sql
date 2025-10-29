-- Fix Acct_Bal table structure to support EOD batch job logic
-- V2: Update Acct_Bal table for proper EOD processing

-- First, create a backup of existing data
CREATE TABLE Acct_Bal_Backup AS SELECT * FROM Acct_Bal;

-- Drop the existing table
DROP TABLE Acct_Bal;

-- Recreate Acct_Bal table with proper structure for EOD processing
CREATE TABLE Acct_Bal (
  Tran_Date DATE NOT NULL,
  Account_No VARCHAR(13) NOT NULL,
  Opening_Bal DECIMAL(20, 2) DEFAULT 0.00,
  DR_Summation DECIMAL(20, 2) DEFAULT 0.00,
  CR_Summation DECIMAL(20, 2) DEFAULT 0.00,
  Closing_Bal DECIMAL(20, 2) DEFAULT 0.00,
  Current_Balance DECIMAL(20, 2) NOT NULL DEFAULT 0.00,
  Available_Balance DECIMAL(20, 2) NOT NULL DEFAULT 0.00,
  Last_Updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (Tran_Date, Account_No),
  FOREIGN KEY (Account_No) REFERENCES Cust_Acct_Master(Account_No)
);

-- Migrate existing data to new structure
-- For each account, create a record with today's date and current balance as closing balance
INSERT INTO Acct_Bal (Tran_Date, Account_No, Opening_Bal, DR_Summation, CR_Summation, Closing_Bal, Current_Balance, Available_Balance, Last_Updated)
SELECT 
    CURDATE() as Tran_Date,
    Account_No,
    Current_Balance as Opening_Bal,
    0.00 as DR_Summation,
    0.00 as CR_Summation,
    Current_Balance as Closing_Bal,
    Current_Balance,
    Available_Balance,
    Last_Updated
FROM Acct_Bal_Backup;

-- Drop the backup table
DROP TABLE Acct_Bal_Backup;
