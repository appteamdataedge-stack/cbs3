-- Verification script for Money Market Database
USE moneymarketdb;

-- Check all tables exist
SELECT 'Database Tables' as Check_Type;
SHOW TABLES;

-- Check record counts
SELECT 'Record Counts Summary' as Summary;
SELECT 'Customers' as Table_Name, COUNT(*) as Record_Count FROM Cust_Master
UNION ALL
SELECT 'Products', COUNT(*) FROM Prod_Master
UNION ALL  
SELECT 'Sub-Products', COUNT(*) FROM Sub_Prod_Master
UNION ALL
SELECT 'Customer Accounts', COUNT(*) FROM Cust_Acct_Master
UNION ALL
SELECT 'Office Accounts', COUNT(*) FROM OF_Acct_Master
UNION ALL
SELECT 'Transactions', COUNT(*) FROM Tran_Table
UNION ALL
SELECT 'Account Balances', COUNT(*) FROM Acct_Bal
UNION ALL
SELECT 'GL Setup', COUNT(*) FROM GL_setup
UNION ALL
SELECT 'GL Balances', COUNT(*) FROM GL_Balance
UNION ALL
SELECT 'GL Movements', COUNT(*) FROM GL_Movement
UNION ALL
SELECT 'Interest Accruals', COUNT(*) FROM Intt_Accr_Tran;

-- Check data integrity
SELECT 'Data Integrity Checks' as Check_Type;

-- Check for orphaned records
SELECT 'Orphaned Accounts (no customer)' as Check_Description, COUNT(*) as Issues
FROM Cust_Acct_Master cam 
LEFT JOIN Cust_Master cm ON cam.Cust_Id = cm.Cust_Id 
WHERE cm.Cust_Id IS NULL
UNION ALL
SELECT 'Orphaned Accounts (no sub-product)', COUNT(*)
FROM Cust_Acct_Master cam
LEFT JOIN Sub_Prod_Master spm ON cam.Sub_Product_Id = spm.Sub_Product_Id
WHERE spm.Sub_Product_Id IS NULL
UNION ALL
SELECT 'Orphaned Transactions', COUNT(*)
FROM Tran_Table tt
LEFT JOIN Cust_Acct_Master cam ON tt.Account_No = cam.Account_No
WHERE cam.Account_No IS NULL;

-- Sample data verification  
SELECT 'Sample Customer Data' as Data_Sample;
SELECT 
    cm.Cust_Id,
    cm.Ext_Cust_Id,
    cm.Cust_Type,
    COALESCE(CONCAT(cm.First_Name, ' ', cm.Last_Name), cm.Trade_Name) as Customer_Name,
    cm.Branch_Code
FROM Cust_Master cm 
LIMIT 5;

SELECT 'Sample Account Data' as Data_Sample;
SELECT 
    cam.Account_No,
    cam.Acct_Name,
    spm.Sub_Product_Name,
    pm.Product_Name,
    cam.Account_Status,
    FORMAT(ab.Current_Balance, 2) as Balance
FROM Cust_Acct_Master cam
JOIN Sub_Prod_Master spm ON cam.Sub_Product_Id = spm.Sub_Product_Id  
JOIN Prod_Master pm ON spm.Product_Id = pm.Product_Id
LEFT JOIN Acct_Bal ab ON cam.Account_No = ab.Account_No
LIMIT 5;

SELECT 'Sample Transaction Data' as Data_Sample;
SELECT 
    tt.Tran_Id,
    tt.Tran_Date,
    tt.Dr_Cr_Flag,
    tt.Account_No,
    FORMAT(tt.LCY_Amt, 2) as Amount,
    tt.Tran_Status,
    tt.Narration
FROM Tran_Table tt
ORDER BY tt.Tran_Date DESC
LIMIT 5;

-- Balance summary
SELECT 'Balance Summary by Product Type' as Summary;
SELECT 
    pm.Product_Name,
    COUNT(cam.Account_No) as Account_Count,
    FORMAT(SUM(ab.Current_Balance), 2) as Total_Balance,
    FORMAT(AVG(ab.Current_Balance), 2) as Average_Balance
FROM Cust_Acct_Master cam
JOIN Sub_Prod_Master spm ON cam.Sub_Product_Id = spm.Sub_Product_Id
JOIN Prod_Master pm ON spm.Product_Id = pm.Product_Id  
LEFT JOIN Acct_Bal ab ON cam.Account_No = ab.Account_No
WHERE cam.Account_Status = 'Active'
GROUP BY pm.Product_Id, pm.Product_Name
ORDER BY pm.Product_Name;
