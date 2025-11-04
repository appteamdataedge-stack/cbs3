@echo off
echo ========================================
echo Update Table Structure and Populate
echo ========================================
echo.
echo This will:
echo 1. Drop and recreate txn_hist_acct (without Opening_Balance)
echo 2. Populate with correct balance calculation logic
echo.
echo Balance Logic:
echo - First transaction: Use acct_bal or 0
echo - Subsequent: Previous BALANCE_AFTER_TRAN +/- TRAN_AMT
echo.
pause

echo.
echo Step 1: Updating table structure...
mysql -u root -p moneymarketdb < update_txn_hist_acct_structure.sql

echo.
echo Step 2: Populating with correct logic...
mysql -u root -p moneymarketdb < populate_with_correct_logic.sql

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo SUCCESS!
    echo ========================================
    echo.
    echo Table updated and populated successfully!
    echo.
) else (
    echo.
    echo ========================================
    echo ERROR!
    echo ========================================
)

pause

