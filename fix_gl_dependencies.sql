-- Fix GL Dependencies Script
-- This script fixes the remaining GL number inconsistencies

USE moneymarketdb;

-- =====================================================
-- 1. FIX CUSTOMER ACCOUNTS WITH WRONG GL NUMBERS
-- =====================================================

-- Update accounts that still reference old GL numbers
UPDATE Cust_Acct_Master 
SET GL_Num = '110101001'  -- Savings Bank Regular
WHERE GL_Num = '210101001';

UPDATE Cust_Acct_Master 
SET GL_Num = '110101002'  -- Savings Bank (Sr. Citizen)  
WHERE GL_Num = '210101002';

UPDATE Cust_Acct_Master 
SET GL_Num = '110102001'  -- Current Account Regular
WHERE GL_Num = '210102001';

-- =====================================================
-- 2. CREATE MINIMAL OFFICE ACCOUNTS
-- =====================================================

INSERT INTO OF_Acct_Master (Account_No, Sub_Product_Id, GL_Num, Acct_Name, Date_Opening, Date_Closure, Branch_Code, Account_Status, Reconciliation_Required) VALUES 
('OF130101001', 1, '130101001', 'Interest Payable Account', CURDATE(), NULL, 'BR001', 'Active', TRUE),
('OF240101001', 2, '240101001', 'Interest Expenditure Account', CURDATE(), NULL, 'BR001', 'Active', TRUE),
('OF140101001', 3, '140101001', 'Interest Income Account', CURDATE(), NULL, 'BR001', 'Active', TRUE);

-- =====================================================
-- 3. CREATE SAMPLE TRANSACTIONS
-- =====================================================

-- Insert sample transactions
INSERT INTO Tran_Table (Tran_Id, Tran_Date, Value_Date, Dr_Cr_Flag, Tran_Status, Account_No, Tran_Ccy, FCY_Amt, Exchange_Rate, LCY_Amt, Debit_Amount, Credit_Amount, Narration, UDF1, Pointing_Id) VALUES 
('TXN001', CURDATE(), CURDATE(), 'C', 'Verified', 
 '110101001001', 
 'USD', 10000.00, 1.0000, 10000.00, NULL, 10000.00, 'Sample Deposit', 'TEST', 1001),

('TXN002', CURDATE(), CURDATE(), 'D', 'Verified',
 '110101001002', 
 'USD', 15000.00, 1.0000, 15000.00, 15000.00, NULL, 'Sample Loan', 'TEST', 1002);

-- =====================================================
-- 4. CREATE GL MOVEMENTS
-- =====================================================

INSERT INTO GL_Movement (Tran_Id, GL_Num, Dr_Cr_Flag, Tran_Date, Value_Date, Amount, Balance_After) VALUES 
('TXN001', '110101001', 'C', CURDATE(), CURDATE(), 10000.00, 10000.00),
('TXN002', '210201001', 'D', CURDATE(), CURDATE(), 15000.00, 15000.00);

-- =====================================================
-- 5. UPDATE GL BALANCES
-- =====================================================

UPDATE GL_Balance 
SET Current_Balance = CASE GL_Num
    WHEN '110101001' THEN 10000.00
    WHEN '210201001' THEN 15000.00
    ELSE Current_Balance
END
WHERE GL_Num IN ('110101001', '210201001');

-- Display final summary
SELECT 'GL Dependencies Fixed Successfully!' as Status;
SELECT 'Updated Accounts:', COUNT(*) FROM Cust_Acct_Master WHERE GL_Num LIKE '11%' OR GL_Num LIKE '21%'
UNION ALL
SELECT 'Office Accounts:', COUNT(*) FROM OF_Acct_Master
UNION ALL
SELECT 'Transactions:', COUNT(*) FROM Tran_Table
UNION ALL
SELECT 'GL Movements:', COUNT(*) FROM GL_Movement;
