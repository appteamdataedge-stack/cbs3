-- Add missing fields to Product Master table
ALTER TABLE Prod_Master 
ADD COLUMN Deal_Or_Running VARCHAR(10) AFTER Interest_Bearing,
ADD COLUMN Currency VARCHAR(3) DEFAULT 'BDT' AFTER Deal_Or_Running;

-- Update column names to match entity (fix case sensitivity)
ALTER TABLE Prod_Master 
CHANGE COLUMN Customer_Product Customer_Product_Flag TINYINT(1) DEFAULT 1,
CHANGE COLUMN Interest_Bearing Interest_Bearing_Flag TINYINT(1) DEFAULT 0;
