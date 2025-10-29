-- =====================================================
-- Quick Fix: Drop Tran_Id from Intt_Accr_Tran
-- =====================================================

USE moneymarketdb;

-- Drop Tran_Id column from Intt_Accr_Tran
ALTER TABLE Intt_Accr_Tran DROP COLUMN Tran_Id;

-- Verify the change
DESCRIBE Intt_Accr_Tran;
