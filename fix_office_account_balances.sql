-- Manually add missing Acct_Bal records for office accounts
-- This fixes the 404 error for office account balance API

INSERT INTO Acct_Bal (Tran_Date, Account_No, Opening_Bal, DR_Summation, CR_Summation, Closing_Bal, Current_Balance, Available_Balance, Last_Updated)
VALUES 
    (CURDATE(), '911010100102', 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, NOW()),
    (CURDATE(), '921020100101', 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, NOW())
ON DUPLICATE KEY UPDATE
    Last_Updated = NOW();
