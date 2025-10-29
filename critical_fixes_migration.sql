-- =====================================================
-- Critical Fixes Migration Script
-- =====================================================
-- Purpose:
-- 1. Change Accr_Tran_Id from BIGINT AUTO_INCREMENT to VARCHAR(20) for custom ID generation
-- 2. Add Tran_Id column to gl_movement_accrual table
-- 3. Add unique constraint to prevent duplicate accrual GL movements
-- 4. Update foreign key relationships
-- =====================================================

USE moneymarketdb;

-- =====================================================
-- STEP 1: Clear existing test data
-- =====================================================
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE gl_movement_accrual;
TRUNCATE TABLE intt_accr_tran;
SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================
-- STEP 2: Drop FK constraint first
-- =====================================================

-- Drop existing FK constraint on Accr_Tran_Id
ALTER TABLE gl_movement_accrual
DROP FOREIGN KEY gl_movement_accrual_ibfk_2;

-- =====================================================
-- STEP 3: Modify both tables to VARCHAR(20)
-- =====================================================

-- Change intt_accr_tran.Accr_Tran_Id from AUTO_INCREMENT BIGINT to VARCHAR(20)
ALTER TABLE intt_accr_tran
MODIFY COLUMN Accr_Tran_Id VARCHAR(20) NOT NULL;

-- Change gl_movement_accrual.Accr_Tran_Id to VARCHAR(20) to match parent table
ALTER TABLE gl_movement_accrual
MODIFY COLUMN Accr_Tran_Id VARCHAR(20) NOT NULL;

-- Add index for performance on date + ID queries
CREATE INDEX idx_accr_date_id ON intt_accr_tran(Accrual_Date, Accr_Tran_Id);

-- =====================================================
-- STEP 4: Add Tran_Id column to gl_movement_accrual
-- =====================================================

-- Add Tran_Id column (nullable, for future use - NULL for accrual entries)
ALTER TABLE gl_movement_accrual
ADD COLUMN Tran_Id VARCHAR(20) NULL AFTER Tran_Date;

-- Add unique constraint to prevent duplicate accrual GL movements
ALTER TABLE gl_movement_accrual
ADD UNIQUE INDEX idx_unique_accr_tran_id (Accr_Tran_Id);

-- Add index on Tran_Id for performance
CREATE INDEX idx_tran_id ON gl_movement_accrual(Tran_Id);

-- Re-create FK constraint with VARCHAR type
ALTER TABLE gl_movement_accrual
ADD CONSTRAINT fk_glmva_accr_tran
FOREIGN KEY (Accr_Tran_Id) REFERENCES intt_accr_tran(Accr_Tran_Id)
ON DELETE RESTRICT;

-- Add FK constraint to tran_table (optional, for future use)
ALTER TABLE gl_movement_accrual
ADD CONSTRAINT fk_glmva_tran
FOREIGN KEY (Tran_Id) REFERENCES tran_table(Tran_Id)
ON DELETE SET NULL;

-- =====================================================
-- STEP 5: Re-create FK constraints
-- =====================================================

-- Show table structure
DESCRIBE intt_accr_tran;
DESCRIBE gl_movement_accrual;

-- Show constraints
SHOW CREATE TABLE gl_movement_accrual;

-- =====================================================
-- Expected Results:
--
-- intt_accr_tran.Accr_Tran_Id: VARCHAR(20), NOT NULL, PRI
-- gl_movement_accrual.Accr_Tran_Id: VARCHAR(20), NOT NULL, UNI
-- gl_movement_accrual.Tran_Id: VARCHAR(20), NULL, MUL
--
-- Unique constraint: idx_unique_accr_tran_id on Accr_Tran_Id
-- FK constraints:
--   - fk_glmva_accr_tran (Accr_Tran_Id -> intt_accr_tran)
--   - fk_glmva_tran (Tran_Id -> tran_table)
-- =====================================================
