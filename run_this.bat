@echo off
echo Running final correct population...
Get-Content correct_population_final.sql | mysql -u root -p moneymarketdb
pause

