-- Update GL_setup data with the provided hierarchical structure
-- This migration replaces the existing GL setup data with the new structure

-- Disable foreign key checks temporarily
SET FOREIGN_KEY_CHECKS = 0;

-- Clear existing GL setup data and related tables
DELETE FROM Account_Seq;
DELETE FROM GL_Balance;
DELETE FROM GL_Movement;
DELETE FROM GL_Movement_Accrual;
DELETE FROM GL_setup;

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;

-- Insert new GL setup data as provided
INSERT INTO GL_setup (GL_Name, Layer_Id, Layer_GL_Num, Parent_GL_Num, GL_Num) VALUES 
-- Level 0: Main Categories
('Liability', 0, '100000000', '-', '100000000'),
('Asset', 0, '200000000', '-', '200000000'),

-- Level 1: Sub-categories
('Deposits', 1, '10000000', '100000000', '110000000'),
('Office Payable', 1, '30000000', '100000000', '130000000'),
('Income', 1, '40000000', '100000000', '140000000'),
('Expenditure', 1, '40000000', '200000000', '240000000'),
('Loans', 1, '10000000', '200000000', '210000000'),
('Office Liability', 1, '20000000', '100000000', '120000000'),

-- Level 2: Product Types
('Demand Deposit', 2, '0100000', '110000000', '110100000'),
('Interest Income', 2, '0100000', '140000000', '140100000'),
('Interest Expenditure', 2, '0100000', '240000000', '240100000'),
('Interest Payable', 2, '0100000', '130000000', '130100000'),
('Time Deposit', 2, '0200000', '110000000', '110200000'),
('Demand Loans', 2, '0200000', '210000000', '210200000'),

-- Level 3: Specific Account Types
('Savings Bank', 3, '01000', '110100000', '110101000'),
('Interest Expenditure Savings Bank', 3, '01000', '240100000', '240101000'),
('Interest Payable Savings Bank', 3, '01000', '130100000', '130101000'),
('Overdraft Interest Income', 3, '01000', '140100000', '140101000'),
('Current Account', 3, '02000', '110100000', '110102000'),
('Term Deposit', 3, '01000', '110200000', '110201000'),
('Overdraft', 3, '01000', '210200000', '210201000'),

-- Level 4: Specific Product Variants
('Savings Bank Regular', 4, '001', '110101000', '110101001'),
('Interest Expenditure Savings Bank Regular', 4, '001', '240101000', '240101001'),
('Interest Payable Savings Bank Regular', 4, '001', '130101000', '130101001'),
('Savings Bank (Sr. Citizen)', 4, '002', '110101000', '110101002'),
('Current Account Regular', 4, '001', '110102000', '110102001'),
('Term Deposit Cum', 4, '001', '110201000', '110201001'),
('Term Deposit Non Cum', 4, '002', '110201000', '110201002'),
('OD against TD', 4, '001', '210201000', '210201001'),
('OD against TD Interest Income', 4, '001', '140101000', '140101001');

-- Update GL Balance table to include new GL accounts
