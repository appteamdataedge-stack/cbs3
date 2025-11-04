@echo off
echo Creating txn_hist_acct table...
echo.
echo Please enter your MySQL root password when prompted.
echo.

mysql -u root -p moneymarketdb < create_txn_hist_acct_table.sql

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo Table created successfully!
    echo ========================================
    echo.
    echo Verifying table creation...
    mysql -u root -p moneymarketdb -e "DESCRIBE txn_hist_acct;"
    echo.
    mysql -u root -p moneymarketdb -e "SHOW INDEX FROM txn_hist_acct;"
) else (
    echo.
    echo ========================================
    echo Error creating table!
    echo ========================================
    echo Please check the error message above.
)

echo.
pause

