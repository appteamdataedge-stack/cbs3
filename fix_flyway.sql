-- Fix Flyway schema history by removing the failed migration record
-- This will allow the application to start properly

USE moneymarketdb;

-- Remove the failed migration record for version 5
DELETE FROM flyway_schema_history 
WHERE version = '5' AND description LIKE '%create acct bal daily table%';

-- Check the current state
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
