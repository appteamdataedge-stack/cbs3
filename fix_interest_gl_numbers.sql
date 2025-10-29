-- Fix missing interest GL numbers in sub-products
-- This will enable Interest Accrual Transaction Update (Job 2) to work properly

-- Update TEST-111 sub-product (Sub_Product_Id = 25)
UPDATE Sub_Prod_Master 
SET 
    interest_expenditure_gl_num = '240101000',
    interest_receivable_gl_num = '230101000'
WHERE Sub_Product_Id = 25;

-- Update SB-Sav-1 sub-product (Sub_Product_Id = 27)  
UPDATE Sub_Prod_Master 
SET 
    interest_expenditure_gl_num = '240101001',
    interest_receivable_gl_num = '230101001'
WHERE Sub_Product_Id = 27;

-- Verify the updates
SELECT 
    Sub_Product_Id, 
    Sub_Product_Code, 
    Intt_Code,
    interest_payable_gl_num,
    interest_expenditure_gl_num,
    interest_income_gl_num,
    interest_receivable_gl_num
FROM Sub_Prod_Master 
WHERE Intt_Code IS NOT NULL AND Intt_Code != '';
