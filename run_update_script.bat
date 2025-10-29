@echo off
echo This script will update the gl_setup table in your database.
echo Please enter your MySQL password when prompted.

mysql -u root -p moneymarketdb < update_gl_setup.sql

echo.
echo If the script executed successfully, your gl_setup table has been updated.
echo Press any key to exit...
pause > nul
