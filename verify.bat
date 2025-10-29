@echo off
echo Verifying database dependencies...
mysql -u root -pasif@yasir123 moneymarketdb < verify_dependencies.sql
echo Verification completed!
pause
