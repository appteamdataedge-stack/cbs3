@echo off
echo ========================================
echo Checking Transaction Status
echo ========================================
echo.
echo This will show you:
echo 1. How many transactions have each status
echo 2. Sample of all transactions
echo 3. All verified transactions
echo.
pause

mysql -u root -p moneymarketdb < check_transaction_status.sql

echo.
pause

