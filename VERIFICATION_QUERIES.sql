-- ================================================================
-- VERIFICATION QUERIES FOR GL BALANCE UPDATE FIX
-- ================================================================
-- These queries help verify that ALL GLs are being updated daily
-- in the gl_balance table, regardless of transaction activity
-- ================================================================

-- Query 1: Count total GLs in gl_setup (master table)
-- This is the baseline - ALL these GLs should appear in gl_balance daily
SELECT 
    COUNT(*) AS total_gls_in_system,
    'All GLs that should appear in daily balance updates' AS description
FROM gl_setup;

-- Query 2: Count GLs in gl_balance for a specific date
-- After the fix, this should equal the count from Query 1
SELECT 
    Tran_Date,
    COUNT(DISTINCT GL_Num) AS gls_in_balance_table,
    'GLs that have balance records for this date' AS description
FROM gl_balance
WHERE Tran_Date = '2025-10-27'  -- Replace with target date
GROUP BY Tran_Date;

-- Query 3: Compare gl_setup vs gl_balance for a specific date
-- Shows which GLs are MISSING from gl_balance (should be zero after fix)
SELECT 
    gs.GL_Num,
    gs.GL_Name,
    'MISSING from gl_balance table' AS status
FROM gl_setup gs
LEFT JOIN gl_balance gb ON gs.GL_Num = gb.GL_Num AND gb.Tran_Date = '2025-10-27'
WHERE gb.GL_Num IS NULL
ORDER BY gs.GL_Num;

-- Query 4: Show GLs WITH transactions on a specific date
SELECT DISTINCT
    gm.GL_Num,
    'Has transactions in gl_movement' AS source
FROM gl_movement gm
WHERE gm.Tran_Date = '2025-10-27'
UNION
SELECT DISTINCT
    gma.GL_Num,
    'Has transactions in gl_movement_accrual' AS source
FROM gl_movement_accrual gma
WHERE gma.Accrual_Date = '2025-10-27'
ORDER BY GL_Num;

-- Query 5: Show GLs WITHOUT transactions (should still be in gl_balance after fix)
SELECT 
    gb.GL_Num,
    gb.Opening_Bal,
    gb.DR_Summation,
    gb.CR_Summation,
    gb.Closing_Bal,
    CASE 
        WHEN gb.DR_Summation = 0 AND gb.CR_Summation = 0 THEN 'No transactions (Carried Forward)'
        ELSE 'Has transactions'
    END AS transaction_status
FROM gl_balance gb
WHERE gb.Tran_Date = '2025-10-27'
  AND gb.DR_Summation = 0 
  AND gb.CR_Summation = 0
ORDER BY gb.GL_Num;

-- Query 6: Verify Balance Sheet components are all present
-- Assets (2*), Liabilities (1* excluding 14*), Income (14*), Expenditure (24*)
SELECT 
    CASE 
        WHEN gb.GL_Num LIKE '2%' THEN 'ASSETS'
        WHEN gb.GL_Num LIKE '1%' AND gb.GL_Num NOT LIKE '14%' THEN 'LIABILITIES'
        WHEN gb.GL_Num LIKE '14%' THEN 'INCOME'
        WHEN gb.GL_Num LIKE '24%' THEN 'EXPENDITURE'
        ELSE 'OTHER'
    END AS category,
    COUNT(*) AS gl_count,
    SUM(gb.Closing_Bal) AS total_balance
FROM gl_balance gb
WHERE gb.Tran_Date = '2025-10-27'
GROUP BY 
    CASE 
        WHEN gb.GL_Num LIKE '2%' THEN 'ASSETS'
        WHEN gb.GL_Num LIKE '1%' AND gb.GL_Num NOT LIKE '14%' THEN 'LIABILITIES'
        WHEN gb.GL_Num LIKE '14%' THEN 'INCOME'
        WHEN gb.GL_Num LIKE '24%' THEN 'EXPENDITURE'
        ELSE 'OTHER'
    END
ORDER BY category;

-- Query 7: Verify Balance Sheet equation (Assets = Liabilities + Net Profit)
WITH balance_sheet AS (
    SELECT 
        SUM(CASE WHEN gb.GL_Num LIKE '2%' THEN gb.Closing_Bal ELSE 0 END) AS total_assets,
        SUM(CASE WHEN gb.GL_Num LIKE '1%' AND gb.GL_Num NOT LIKE '14%' THEN gb.Closing_Bal ELSE 0 END) AS total_liabilities,
        SUM(CASE WHEN gb.GL_Num LIKE '14%' THEN gb.Closing_Bal ELSE 0 END) AS total_income,
        SUM(CASE WHEN gb.GL_Num LIKE '24%' THEN gb.Closing_Bal ELSE 0 END) AS total_expenditure
    FROM gl_balance gb
    WHERE gb.Tran_Date = '2025-10-27'
)
SELECT 
    total_assets,
    total_liabilities,
    total_income,
    total_expenditure,
    (total_income - total_expenditure) AS net_profit_loss,
    (total_liabilities + (total_income - total_expenditure)) AS liabilities_plus_profit,
    (total_assets - (total_liabilities + (total_income - total_expenditure))) AS difference,
    CASE 
        WHEN ABS(total_assets - (total_liabilities + (total_income - total_expenditure))) < 0.01 
        THEN 'BALANCED ✓'
        ELSE 'IMBALANCED ✗'
    END AS balance_sheet_status
FROM balance_sheet;

-- Query 8: Show sample of GLs with carried-forward balances (no transactions)
SELECT TOP 10
    gb.GL_Num,
    gs.GL_Name,
    gb.Tran_Date,
    gb.Opening_Bal,
    gb.DR_Summation,
    gb.CR_Summation,
    gb.Closing_Bal,
    'Carried forward (no activity)' AS note
FROM gl_balance gb
JOIN gl_setup gs ON gb.GL_Num = gs.GL_Num
WHERE gb.Tran_Date = '2025-10-27'
  AND gb.DR_Summation = 0 
  AND gb.CR_Summation = 0
  AND gb.Opening_Bal = gb.Closing_Bal  -- Balance unchanged
ORDER BY gb.GL_Num;

-- Query 9: Verify Trial Balance (Total DR = Total CR)
SELECT 
    SUM(gb.DR_Summation) AS total_dr,
    SUM(gb.CR_Summation) AS total_cr,
    (SUM(gb.DR_Summation) - SUM(gb.CR_Summation)) AS difference,
    CASE 
        WHEN ABS(SUM(gb.DR_Summation) - SUM(gb.CR_Summation)) < 0.01 
        THEN 'BALANCED ✓'
        ELSE 'IMBALANCED ✗'
    END AS trial_balance_status
FROM gl_balance gb
WHERE gb.Tran_Date = '2025-10-27';

-- Query 10: Historical comparison - Show GL count trend over time
SELECT 
    Tran_Date,
    COUNT(DISTINCT GL_Num) AS gl_count,
    COUNT(*) AS total_records,
    SUM(CASE WHEN DR_Summation = 0 AND CR_Summation = 0 THEN 1 ELSE 0 END) AS gls_with_no_transactions,
    SUM(CASE WHEN DR_Summation > 0 OR CR_Summation > 0 THEN 1 ELSE 0 END) AS gls_with_transactions
FROM gl_balance
WHERE Tran_Date >= DATEADD(day, -7, '2025-10-27')  -- Last 7 days
GROUP BY Tran_Date
ORDER BY Tran_Date DESC;

-- ================================================================
-- EXPECTED RESULTS AFTER FIX:
-- ================================================================
-- Query 1 = Query 2: All GLs in gl_setup should be in gl_balance
-- Query 3: Should return 0 rows (no missing GLs)
-- Query 5: Should show GLs with carried-forward balances
-- Query 7: balance_sheet_status should be 'BALANCED ✓'
-- Query 9: trial_balance_status should be 'BALANCED ✓'
-- Query 10: gl_count should be consistent and equal to total GLs
-- ================================================================

