-- Verify Dependencies Script
-- This script checks the current state of all dependent data

USE moneymarketdb;

-- =====================================================
-- VERIFICATION QUERIES
-- =====================================================

SELECT '=== DEPENDENCY VERIFICATION REPORT ===' as Report;

-- 1. Check GL Setup Data
SELECT '1. GL Setup Data:' as Section;
SELECT Layer_Id, COUNT(*) as Count, GROUP_CONCAT(GL_Num ORDER BY GL_Num) as GL_Numbers
FROM GL_setup 
GROUP BY Layer_Id 
ORDER BY Layer_Id;

-- 2. Check Products and their GL references
SELECT '2. Products and GL References:' as Section;
SELECT Product_Code, Product_Name, Cum_GL_Num,
       CASE WHEN EXISTS(SELECT 1 FROM GL_setup WHERE GL_Num = Cum_GL_Num) 
            THEN 'VALID' ELSE 'INVALID' END as GL_Status
FROM Prod_Master;

-- 3. Check Sub-Products and their GL references
SELECT '3. Sub-Products and GL References:' as Section;
SELECT Sub_Product_Code, Sub_Product_Name, Cum_GL_Num,
       CASE WHEN EXISTS(SELECT 1 FROM GL_setup WHERE GL_Num = Cum_GL_Num) 
            THEN 'VALID' ELSE 'INVALID' END as GL_Status
FROM Sub_Prod_Master;

-- 4. Check Customer Accounts and their GL references
SELECT '4. Customer Accounts and GL References:' as Section;
SELECT COUNT(*) as Total_Accounts,
       COUNT(DISTINCT GL_Num) as Unique_GLs,
       GROUP_CONCAT(DISTINCT GL_Num ORDER BY GL_Num) as GL_Numbers
FROM Cust_Acct_Master;

-- 5. Check Office Accounts
SELECT '5. Office Accounts:' as Section;
SELECT COUNT(*) as Total_Office_Accounts,
       COUNT(DISTINCT GL_Num) as Unique_GLs,
       GROUP_CONCAT(DISTINCT GL_Num ORDER BY GL_Num) as GL_Numbers
FROM OF_Acct_Master;

-- 6. Check Transactions
SELECT '6. Transactions:' as Section;
SELECT COUNT(*) as Total_Transactions,
       COUNT(DISTINCT Account_No) as Unique_Accounts
FROM Tran_Table;

-- 7. Check GL Movements
SELECT '7. GL Movements:' as Section;
SELECT COUNT(*) as Total_Movements,
       COUNT(DISTINCT GL_Num) as Unique_GLs,
       GROUP_CONCAT(DISTINCT GL_Num ORDER BY GL_Num) as GL_Numbers
FROM GL_Movement;

-- 8. Check GL Balances
SELECT '8. GL Balances:' as Section;
SELECT COUNT(*) as Total_Balances,
       SUM(Current_Balance) as Total_Balance_Amount
FROM GL_Balance;

-- 9. Check Account Sequences
SELECT '9. Account Sequences:' as Section;
SELECT COUNT(*) as Total_Sequences,
       GROUP_CONCAT(GL_Num ORDER BY GL_Num) as GL_Numbers
FROM Account_Seq;

-- 10. Summary of Valid Dependencies
SELECT '10. Dependency Summary:' as Section;
SELECT 
    'Products with Valid GL' as Check_Type,
    COUNT(*) as Count
FROM Prod_Master p
WHERE EXISTS(SELECT 1 FROM GL_setup g WHERE g.GL_Num = p.Cum_GL_Num)

UNION ALL

SELECT 
    'Sub-Products with Valid GL',
    COUNT(*)
FROM Sub_Prod_Master sp
WHERE EXISTS(SELECT 1 FROM GL_setup g WHERE g.GL_Num = sp.Cum_GL_Num)

UNION ALL

SELECT 
    'Customer Accounts with Valid GL',
    COUNT(*)
FROM Cust_Acct_Master cam
WHERE EXISTS(SELECT 1 FROM GL_setup g WHERE g.GL_Num = cam.GL_Num)

UNION ALL

SELECT 
    'Office Accounts with Valid GL',
    COUNT(*)
FROM OF_Acct_Master oam
WHERE EXISTS(SELECT 1 FROM GL_setup g WHERE g.GL_Num = oam.GL_Num);

SELECT '=== END OF VERIFICATION REPORT ===' as Report;
