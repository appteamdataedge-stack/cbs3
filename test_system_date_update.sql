-- Test script to update System_Date to 2025-01-01
-- This will test if the EOD page shows the correct date from parameter table

UPDATE Parameter_Table 
SET Parameter_Value = '2025-01-01',
    Last_Updated = NOW(),
    Updated_By = 'TEST'
WHERE Parameter_Name = 'System_Date';

-- Verify the update
SELECT Parameter_Name, Parameter_Value, Last_Updated, Updated_By 
FROM Parameter_Table 
WHERE Parameter_Name = 'System_Date';
