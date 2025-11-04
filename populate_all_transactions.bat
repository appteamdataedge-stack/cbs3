@echo off
echo ========================================
echo Populate with ALL Transactions
echo ========================================
echo.
echo This will populate txn_hist_acct with ALL transactions
echo (not just Verified ones)
echo.
echo This includes: Entry, Posted, and Verified status
echo.
pause

mysql -u root -p moneymarketdb < populate_all_transactions.sql

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo SUCCESS!
    echo ========================================
    echo.
    echo All transactions have been added to txn_hist_acct
    echo.
) else (
    echo.
    echo ========================================
    echo ERROR!
    echo ========================================
)

pause

