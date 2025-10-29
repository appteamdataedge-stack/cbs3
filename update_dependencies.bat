@echo off
echo Updating database dependencies to work with new GL setup...
echo.

mysql -u root -p moneymarketdb < update_dependencies.sql

echo.
echo Dependencies update completed!
echo Press any key to exit...
pause > nul
