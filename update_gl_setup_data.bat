@echo off
echo This script will update the gl_setup table in your database.

cd moneymarket
call mvn exec:java -Dexec.mainClass="com.example.moneymarket.util.GLSetupDataUpdater"

echo.
echo If the script executed successfully, your gl_setup table has been updated.
echo Press any key to exit...
pause > nul
