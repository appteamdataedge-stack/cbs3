@echo off
echo ========================================
echo Direct Insert - All Verified Transactions
echo ========================================
echo.
echo This will insert all 92 verified transactions
echo using a simple direct INSERT statement.
echo.
pause

mysql -u root -p moneymarketdb < simple_direct_insert.sql

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

