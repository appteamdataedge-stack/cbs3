@echo off
echo Running simple population...
mysql -u root -p moneymarketdb < populate_txn_hist_simple.sql
pause

