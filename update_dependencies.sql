-- Update Dependencies Script
-- This script updates all dependent data to work with the new GL setup structure

USE moneymarketdb;

-- Disable foreign key checks temporarily
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================
-- 1. UPDATE PRODUCTS TO USE NEW GL NUMBERS
-- =====================================================

-- Update Product Master to use new GL numbers from our setup
UPDATE Prod_Master 
SET Cum_GL_Num = '110101000'  -- Savings Bank
WHERE Product_Code = 'MM-DEP';

UPDATE Prod_Master 
SET Cum_GL_Num = '110102000'  -- Current Account
WHERE Product_Code = 'CM-DEP';

UPDATE Prod_Master 
SET Cum_GL_Num = '210201000'  -- Overdraft
WHERE Product_Code = 'MM-LOAN';

UPDATE Prod_Master 
SET Cum_GL_Num = '210201000'  -- Overdraft
WHERE Product_Code = 'CM-LOAN';

-- =====================================================
-- 2. UPDATE SUB-PRODUCTS TO USE NEW GL NUMBERS
-- =====================================================

-- Update Sub-Product Master to use new GL numbers
UPDATE Sub_Prod_Master 
SET Cum_GL_Num = '110101001'  -- Savings Bank Regular
WHERE Sub_Product_Code = 'OND';

UPDATE Sub_Prod_Master 
SET Cum_GL_Num = '110101002'  -- Savings Bank (Sr. Citizen)
WHERE Sub_Product_Code = 'WCD';

UPDATE Sub_Prod_Master 
SET Cum_GL_Num = '110102001'  -- Current Account Regular
WHERE Sub_Product_Code = 'MTD';

UPDATE Sub_Prod_Master 
SET Cum_GL_Num = '110102001'  -- Current Account Regular
WHERE Sub_Product_Code = 'QTD';

UPDATE Sub_Prod_Master 
SET Cum_GL_Num = '210201001'  -- OD against TD
WHERE Sub_Product_Code = 'ONL';

UPDATE Sub_Prod_Master 
SET Cum_GL_Num = '210201001'  -- OD against TD
WHERE Sub_Product_Code = 'WCL';

UPDATE Sub_Prod_Master 
SET Cum_GL_Num = '210201001'  -- OD against TD
WHERE Sub_Product_Code = 'MTL';

UPDATE Sub_Prod_Master 
SET Cum_GL_Num = '210201001'  -- OD against TD
WHERE Sub_Product_Code = 'QTL';

-- =====================================================
-- 3. UPDATE CUSTOMER ACCOUNTS TO USE NEW GL NUMBERS
-- =====================================================

-- Update Customer Account Master GL numbers
UPDATE Cust_Acct_Master 
SET GL_Num = '110101001'  -- Savings Bank Regular
WHERE GL_Num = '110101001';

UPDATE Cust_Acct_Master 
SET GL_Num = '110101002'  -- Savings Bank (Sr. Citizen)
WHERE GL_Num = '110101002';

UPDATE Cust_Acct_Master 
SET GL_Num = '110102001'  -- Current Account Regular
WHERE GL_Num = '110102001';

UPDATE Cust_Acct_Master 
SET GL_Num = '210201001'  -- OD against TD
WHERE GL_Num = '210200001';

-- =====================================================
-- 4. CREATE OFFICE ACCOUNTS FOR NEW GL STRUCTURE
-- =====================================================

-- Insert Office Accounts for key GL accounts
INSERT INTO OF_Acct_Master (Account_No, Sub_Product_Id, GL_Num, Acct_Name, Date_Opening, Date_Closure, Branch_Code, Account_Status, Reconciliation_Required) VALUES 
-- Interest Payable Accounts
('OF130101001', 1, '130101001', 'Interest Payable Savings Bank Regular', CURDATE(), NULL, 'BR001', 'Active', TRUE),
('OF130101002', 2, '130101001', 'Interest Payable Savings Bank Regular', CURDATE(), NULL, 'BR001', 'Active', TRUE),

-- Interest Expenditure Accounts  
('OF240101001', 3, '240101001', 'Interest Expenditure Savings Bank Regular', CURDATE(), NULL, 'BR001', 'Active', TRUE),
('OF240101002', 4, '240101001', 'Interest Expenditure Savings Bank Regular', CURDATE(), NULL, 'BR001', 'Active', TRUE),

-- Interest Income Accounts
('OF140101001', 5, '140101001', 'Overdraft Interest Income', CURDATE(), NULL, 'BR001', 'Active', TRUE),
('OF140101002', 6, '140101001', 'Overdraft Interest Income', CURDATE(), NULL, 'BR001', 'Active', TRUE),

-- OD against TD Interest Income
('OF140101003', 7, '140101001', 'OD against TD Interest Income', CURDATE(), NULL, 'BR001', 'Active', TRUE),
('OF140101004', 8, '140101001', 'OD against TD Interest Income', CURDATE(), NULL, 'BR001', 'Active', TRUE);

-- =====================================================
-- 5. UPDATE ACCOUNT BALANCES
-- =====================================================

-- Update existing account balances to use correct GL numbers
UPDATE Acct_Bal ab
JOIN Cust_Acct_Master cam ON ab.Account_No = cam.Account_No
SET ab.Current_Balance = CASE 
    WHEN cam.GL_Num = '110101001' THEN 5000000.00  -- Savings Bank Regular
    WHEN cam.GL_Num = '110101002' THEN 3000000.00  -- Savings Bank (Sr. Citizen)
    WHEN cam.GL_Num = '110102001' THEN 8000000.00  -- Current Account Regular
    WHEN cam.GL_Num = '210201001' THEN 10000000.00 -- OD against TD
    ELSE ab.Current_Balance
END,
ab.Available_Balance = ab.Current_Balance;

-- =====================================================
-- 6. CREATE SAMPLE TRANSACTIONS FOR NEW GL STRUCTURE
-- =====================================================

-- Insert sample transactions that reference the new GL structure
INSERT INTO Tran_Table (Tran_Id, Tran_Date, Value_Date, Dr_Cr_Flag, Tran_Status, Account_No, Tran_Ccy, FCY_Amt, Exchange_Rate, LCY_Amt, Debit_Amount, Credit_Amount, Narration, UDF1, Pointing_Id) VALUES 
-- Deposit transactions (Credit entries)
(CONCAT('TXN', DATE_FORMAT(CURDATE(), '%Y%m%d'), '001'), CURDATE(), CURDATE(), 'C', 'Verified', 
 (SELECT Account_No FROM Cust_Acct_Master WHERE GL_Num = '110101001' LIMIT 1), 
 'USD', 5000000.00, 1.0000, 5000000.00, NULL, 5000000.00, 'Savings Bank Deposit', 'DEPOSIT', 1001),

(CONCAT('TXN', DATE_FORMAT(CURDATE(), '%Y%m%d'), '002'), CURDATE(), CURDATE(), 'C', 'Verified',
 (SELECT Account_No FROM Cust_Acct_Master WHERE GL_Num = '110102001' LIMIT 1), 
 'USD', 3000000.00, 1.0000, 3000000.00, NULL, 3000000.00, 'Current Account Deposit', 'DEPOSIT', 1002),

-- Loan disbursement transactions (Debit entries)
(CONCAT('TXN', DATE_FORMAT(CURDATE(), '%Y%m%d'), '003'), CURDATE(), CURDATE(), 'D', 'Verified',
 (SELECT Account_No FROM Cust_Acct_Master WHERE GL_Num = '210201001' LIMIT 1), 
 'USD', 10000000.00, 1.0000, 10000000.00, 10000000.00, NULL, 'Overdraft Loan Disbursement', 'LOAN', 1003);

-- =====================================================
-- 7. CREATE GL MOVEMENTS FOR TRANSACTIONS
-- =====================================================

-- Insert GL movements corresponding to transactions
INSERT INTO GL_Movement (Tran_Id, GL_Num, Dr_Cr_Flag, Tran_Date, Value_Date, Amount, Balance_After) VALUES 
-- Deposit GL movements (Credit to liability accounts)
(CONCAT('TXN', DATE_FORMAT(CURDATE(), '%Y%m%d'), '001'), '110101001', 'C', CURDATE(), CURDATE(), 5000000.00, 5000000.00),
(CONCAT('TXN', DATE_FORMAT(CURDATE(), '%Y%m%d'), '002'), '110102001', 'C', CURDATE(), CURDATE(), 3000000.00, 3000000.00),

-- Loan disbursement GL movements (Debit to asset accounts)
(CONCAT('TXN', DATE_FORMAT(CURDATE(), '%Y%m%d'), '003'), '210201001', 'D', CURDATE(), CURDATE(), 10000000.00, 10000000.00);

-- =====================================================
-- 8. UPDATE GL BALANCES
-- =====================================================

-- Update GL balances based on transactions
UPDATE GL_Balance 
SET Current_Balance = CASE GL_Num
    WHEN '110101001' THEN 5000000.00   -- Savings Bank Regular
    WHEN '110102001' THEN 3000000.00   -- Current Account Regular  
    WHEN '210201001' THEN 10000000.00  -- OD against TD
    ELSE Current_Balance
END
WHERE GL_Num IN ('110101001', '110102001', '210201001');

-- =====================================================
-- 9. CREATE SAMPLE INTEREST ACCRUAL TRANSACTIONS
-- =====================================================

-- Insert sample interest accrual transactions
INSERT INTO Intt_Accr_Tran (Tran_Id, Account_No, Accrual_Date, Interest_Rate, Amount, Status) VALUES 
(CONCAT('TXN', DATE_FORMAT(CURDATE(), '%Y%m%d'), '001'), 
 (SELECT Account_No FROM Cust_Acct_Master WHERE GL_Num = '110101001' LIMIT 1), 
 CURDATE(), 4.75, 195.83, 'Pending'),

(CONCAT('TXN', DATE_FORMAT(CURDATE(), '%Y%m%d'), '003'), 
 (SELECT Account_No FROM Cust_Acct_Master WHERE GL_Num = '210201001' LIMIT 1), 
 CURDATE(), 6.25, 1712.33, 'Pending');

-- =====================================================
-- 10. CREATE GL MOVEMENTS FOR ACCRUAL
-- =====================================================

-- Insert GL movements for interest accrual
INSERT INTO GL_Movement_Accrual (Accr_Id, GL_Num, Dr_Cr_Flag, Accrual_Date, Amount, Status) VALUES 
-- Interest payable (Credit to liability)
((SELECT Accr_Id FROM Intt_Accr_Tran WHERE Tran_Id = CONCAT('TXN', DATE_FORMAT(CURDATE(), '%Y%m%d'), '001')), 
 '130101001', 'C', CURDATE(), 195.83, 'Pending'),

-- Interest income (Credit to income)
((SELECT Accr_Id FROM Intt_Accr_Tran WHERE Tran_Id = CONCAT('TXN', DATE_FORMAT(CURDATE(), '%Y%m%d'), '003')), 
 '140101001', 'C', CURDATE(), 1712.33, 'Pending');

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;

-- Display summary
SELECT 'Dependencies Updated Successfully!' as Status;
SELECT 'Updated Tables:' as Summary;
SELECT 'Products Updated:', COUNT(*) FROM Prod_Master WHERE Cum_GL_Num IN ('110101000', '110102000', '210201000')
UNION ALL
SELECT 'Sub-Products Updated:', COUNT(*) FROM Sub_Prod_Master WHERE Cum_GL_Num LIKE '11%' OR Cum_GL_Num LIKE '21%'
UNION ALL
SELECT 'Customer Accounts Updated:', COUNT(*) FROM Cust_Acct_Master
UNION ALL
SELECT 'Office Accounts Created:', COUNT(*) FROM OF_Acct_Master
UNION ALL
SELECT 'Transactions Created:', COUNT(*) FROM Tran_Table
UNION ALL
SELECT 'GL Movements Created:', COUNT(*) FROM GL_Movement
UNION ALL
SELECT 'Interest Accruals Created:', COUNT(*) FROM Intt_Accr_Tran;
