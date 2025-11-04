@echo off
echo ========================================
echo Creating and Populating TXN_HIST_ACCT
echo ========================================
echo.
echo This script will:
echo 1. Create the txn_hist_acct table
echo 2. Populate it with historical data from tran_table
echo 3. Calculate balances for all transactions
echo.
echo Please enter your MySQL root password when prompted.
echo.
pause
echo.

mysql -u root -p moneymarketdb < create_and_populate_txn_hist_acct.sql

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo SUCCESS!
    echo ========================================
    echo.
    echo Table created and populated successfully!
    echo.
    echo You can now use the Statement of Accounts module.
    echo.
) else (
    echo.
    echo ========================================
    echo ERROR!
    echo ========================================
    echo.
    echo Something went wrong. Please check the error message above.
    echo.
)

pause

