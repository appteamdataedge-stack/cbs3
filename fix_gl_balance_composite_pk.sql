-- ==================================================================
-- Migration: Fix GL Balance Primary Key - Change to Composite PK
-- Issue: gl_balance has PK (GL_Num) but needs PK (GL_Num, Tran_date)
-- Root Cause: Single PK prevents multiple daily balance records per GL
-- Date: 2025-01-12
-- ==================================================================

USE moneymarketdb;

-- Step 1: Drop the foreign key constraint (gl_balance references gl_setup)
ALTER TABLE gl_balance DROP FOREIGN KEY gl_balance_ibfk_1;

-- Step 2: Drop the existing primary key
ALTER TABLE gl_balance DROP PRIMARY KEY;

-- Step 3: Make Tran_date NOT NULL (required for composite PK)
ALTER TABLE gl_balance MODIFY COLUMN Tran_date DATE NOT NULL;

-- Step 4: Add composite primary key (GL_Num, Tran_date)
ALTER TABLE gl_balance ADD PRIMARY KEY (GL_Num, Tran_date);

-- Step 5: Re-add the foreign key constraint
ALTER TABLE gl_balance
ADD CONSTRAINT gl_balance_ibfk_1
FOREIGN KEY (GL_Num) REFERENCES gl_setup(GL_Num);

-- Step 6: Verify the change
SHOW CREATE TABLE gl_balance\G

SELECT 'Migration completed successfully! gl_balance now has composite PK (GL_Num, Tran_date)' as Status;
