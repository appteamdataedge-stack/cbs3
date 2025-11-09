-- ========================================
-- VALUE DATING SETUP SCRIPT
-- Run this script to set up value dating functionality
-- ========================================

-- Step 1: Create Tran_Value_Date_Log table
CREATE TABLE IF NOT EXISTS Tran_Value_Date_Log (
    Log_Id BIGINT AUTO_INCREMENT PRIMARY KEY,
    Tran_Id VARCHAR(20) NOT NULL,
    Value_Date DATE NOT NULL,
    Days_Difference INT NOT NULL COMMENT 'Positive for past-dated, negative for future-dated',
    Delta_Interest_Amt DECIMAL(20, 4) DEFAULT 0.0000 COMMENT 'Calculated delta interest amount',
    Adjustment_Posted_Flag VARCHAR(1) DEFAULT 'N' COMMENT 'Y=Posted, N=Pending',
    Created_Timestamp DATETIME NOT NULL,

    INDEX idx_tran_id (Tran_Id),
    INDEX idx_value_date (Value_Date),
    INDEX idx_posted_flag (Adjustment_Posted_Flag),
    INDEX idx_value_date_flag (Value_Date, Adjustment_Posted_Flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Logs value-dated transactions and interest adjustments';

-- Step 2: Insert required parameters
INSERT INTO Parameter_Table (Parameter_Name, Parameter_Value, Parameter_Description, Last_Updated, Updated_By)
VALUES
    ('Past_Value_Date_Limit_Days', '90', 'Maximum days in past for value dating', NOW(), 'SYSTEM'),
    ('Future_Value_Date_Limit_Days', '30', 'Maximum days in future for value dating', NOW(), 'SYSTEM'),
    ('Interest_Default_Divisor', '36500', 'Divisor for interest calculation (365 * 100)', NOW(), 'SYSTEM'),
    ('Last_EOM_Date', '2024-12-31', 'Last End of Month date (update after month-end closing)', NOW(), 'SYSTEM')
ON DUPLICATE KEY UPDATE
    Parameter_Description = VALUES(Parameter_Description),
    Last_Updated = NOW(),
    Updated_By = 'SYSTEM';

-- Step 3: Verify setup
SELECT '=== VERIFICATION ===' as Status;

SELECT 'Table Created' as Status, COUNT(*) as Exists
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name = 'Tran_Value_Date_Log';

SELECT 'Parameters Configured' as Status, COUNT(*) as Count
FROM Parameter_Table
WHERE Parameter_Name IN (
    'Past_Value_Date_Limit_Days',
    'Future_Value_Date_Limit_Days',
    'Interest_Default_Divisor',
    'Last_EOM_Date'
);

SELECT '=== CONFIGURATION VALUES ===' as Status;

SELECT
    Parameter_Name,
    Parameter_Value,
    Parameter_Description
FROM Parameter_Table
WHERE Parameter_Name IN (
    'Past_Value_Date_Limit_Days',
    'Future_Value_Date_Limit_Days',
    'Interest_Default_Divisor',
    'Last_EOM_Date'
)
ORDER BY Parameter_Name;

-- ========================================
-- OPTIONAL: Sample sub-product configuration
-- ========================================
-- Update this section with your actual GL accounts

/*
-- Example for Savings Account (Liability - GL starts with '1')
UPDATE Sub_Prod_Master
SET
    -- Debit side: Interest Expense GL
    interest_receivable_expenditure_gl_num = '240101001',
    -- Credit side: Accrued Interest Payable GL
    interest_income_payable_gl_num = '130101001'
WHERE Sub_Product_Code = 'SAV001';

-- Example for Loan Account (Asset - GL starts with '2')
UPDATE Sub_Prod_Master
SET
    -- Debit side: Accrued Interest Receivable GL
    interest_receivable_expenditure_gl_num = '120201001',
    -- Credit side: Interest Income GL
    interest_income_payable_gl_num = '310201001'
WHERE Sub_Product_Code = 'LOAN001';
*/

-- ========================================
-- MONITORING QUERIES
-- ========================================

-- Check pending future-dated transactions
SELECT '=== PENDING FUTURE-DATED TRANSACTIONS ===' as Status;

SELECT
    t.Tran_Id,
    t.Tran_Date,
    t.Value_Date,
    t.Account_No,
    t.LCY_Amt,
    DATEDIFF(t.Value_Date, (SELECT Parameter_Value FROM Parameter_Table WHERE Parameter_Name = 'System_Date')) as Days_Until_Posting
FROM Tran_Table t
WHERE t.Tran_Status = 'Future'
ORDER BY t.Value_Date;

-- Check recent past-dated transactions with interest adjustments
SELECT '=== RECENT PAST-DATED TRANSACTIONS ===' as Status;

SELECT
    t.Tran_Id,
    t.Tran_Date,
    t.Value_Date,
    t.Account_No,
    t.LCY_Amt,
    v.Days_Difference,
    v.Delta_Interest_Amt,
    v.Adjustment_Posted_Flag
FROM Tran_Table t
INNER JOIN Tran_Value_Date_Log v ON t.Tran_Id = v.Tran_Id
WHERE v.Days_Difference > 0
  AND t.Tran_Date >= DATE_SUB(NOW(), INTERVAL 7 DAY)
ORDER BY t.Tran_Date DESC
LIMIT 20;

-- Check value dating statistics
SELECT '=== VALUE DATING STATISTICS ===' as Status;

SELECT
    'Total Value-Dated Transactions' as Metric,
    COUNT(*) as Count
FROM Tran_Value_Date_Log
UNION ALL
SELECT
    'Past-Dated (Posted)' as Metric,
    COUNT(*) as Count
FROM Tran_Value_Date_Log
WHERE Days_Difference > 0 AND Adjustment_Posted_Flag = 'Y'
UNION ALL
SELECT
    'Future-Dated (Pending)' as Metric,
    COUNT(*) as Count
FROM Tran_Value_Date_Log
WHERE Days_Difference < 0 AND Adjustment_Posted_Flag = 'N'
UNION ALL
SELECT
    'Total Interest Adjustments' as Metric,
    CONCAT(FORMAT(SUM(Delta_Interest_Amt), 2), ' BDT') as Count
FROM Tran_Value_Date_Log
WHERE Delta_Interest_Amt > 0;

-- ========================================
-- END OF SETUP SCRIPT
-- ========================================
