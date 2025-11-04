@echo off
echo ========================================
echo Populate with CORRECT Running Balance
echo ========================================
echo.
echo This will:
echo 1. Clear existing data
echo 2. Populate with correct running balance logic
echo.
echo Balance Logic:
echo - First transaction per account: From acct_bal or 0
echo - Subsequent: Previous BALANCE_AFTER_TRAN +/- TRAN_AMT
echo.
pause

mysql -u root -p moneymarketdb < populate_with_running_balance.sql

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo SUCCESS!
    echo ========================================
) else (
    echo.
    echo ========================================
    echo ERROR!
    echo ========================================
)

pause

