@echo off
echo FINAL Working Population Script
echo.
Get-Content FINAL_WORKING_POPULATE.sql | mysql -u root -p moneymarketdb
pause
