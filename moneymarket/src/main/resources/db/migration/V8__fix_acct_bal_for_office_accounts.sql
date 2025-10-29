-- Fix Acct_Bal foreign key constraint to support both customer and office accounts
-- V8: Update Acct_Bal table to support office accounts

-- First, drop the existing foreign key constraint
ALTER TABLE Acct_Bal DROP FOREIGN KEY acct_bal_ibfk_1;

-- Add missing Acct_Bal records for existing office accounts
INSERT INTO Acct_Bal (Tran_Date, Account_No, Opening_Bal, DR_Summation, CR_Summation, Closing_Bal, Current_Balance, Available_Balance, Last_Updated)
SELECT 
    CURDATE() as Tran_Date,
    oam.Account_No,
    0.00 as Opening_Bal,
    0.00 as DR_Summation,
    0.00 as CR_Summation,
    0.00 as Closing_Bal,
    0.00 as Current_Balance,
    0.00 as Available_Balance,
    NOW() as Last_Updated
FROM OF_Acct_Master oam
LEFT JOIN Acct_Bal ab ON oam.Account_No = ab.Account_No AND ab.Tran_Date = CURDATE()
WHERE ab.Account_No IS NULL
  AND oam.Account_No LIKE '9%'  -- Only office accounts (starting with '9')
  AND oam.Account_Status = 'Active';

-- Note: We cannot add a foreign key constraint that references multiple tables
-- The application logic will need to ensure data integrity for both customer and office accounts
-- This is a common pattern in banking systems where account numbers are unique across all account types
