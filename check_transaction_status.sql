-- Check transaction status distribution in tran_table

USE moneymarketdb;

-- Count transactions by status
SELECT 
    'Transaction Status Distribution' AS Info;
    
SELECT 
    Tran_Status,
    COUNT(*) AS Count
FROM tran_table
GROUP BY Tran_Status
ORDER BY COUNT(*) DESC;

-- Show all transactions (first 20)
SELECT 
    'All Transactions (First 20)' AS Info;

SELECT 
    Tran_Id,
    Account_No,
    Tran_Date,
    Tran_Status,
    Dr_Cr_Flag,
    LCY_Amt,
    Narration
FROM tran_table
ORDER BY Tran_Date DESC
LIMIT 20;

-- Show only verified transactions
SELECT 
    'Verified Transactions Only' AS Info;

SELECT 
    Tran_Id,
    Account_No,
    Tran_Date,
    Tran_Status,
    Dr_Cr_Flag,
    LCY_Amt,
    Narration
FROM tran_table
WHERE Tran_Status = 'Verified'
ORDER BY Tran_Date DESC;

