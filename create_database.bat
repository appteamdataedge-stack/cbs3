@echo off
echo Creating Money Market Database with Dummy Data...
echo.

REM Check if MySQL is available
mysql --version >nul 2>&1
if errorlevel 1 (
    echo Error: MySQL client not found in PATH
    echo Please ensure MySQL is installed and added to PATH
    pause
    exit /b 1
)

REM Execute the SQL script
echo Connecting to MySQL and executing database creation script...
mysql -u root -pasif@yasir123 < create_database_with_data.sql

if errorlevel 1 (
    echo.
    echo Error: Failed to execute database creation script
    echo Please check your MySQL credentials and connection
    pause
    exit /b 1
) else (
    echo.
    echo SUCCESS: Database 'moneymarketdb' created successfully with dummy data!
    echo.
    echo You can now start the Spring Boot application.
)

echo.
pause
