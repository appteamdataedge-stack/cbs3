@echo off
echo Fixing GL dependencies...
mysql -u root -pasif@yasir123 moneymarketdb < fix_gl_dependencies.sql
echo Dependencies fixed!
pause
