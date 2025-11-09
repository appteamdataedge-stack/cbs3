-- Insert required parameters for value dating functionality
-- These parameters control validation rules and interest calculations

-- Past Value Date Limit (default: 90 days)
INSERT INTO Parameter_Table (Parameter_Name, Parameter_Value, Parameter_Description, Last_Updated, Updated_By)
VALUES ('Past_Value_Date_Limit_Days', '90', 'Maximum days in past for value dating', NOW(), 'SYSTEM')
ON DUPLICATE KEY UPDATE
    Parameter_Description = 'Maximum days in past for value dating',
    Last_Updated = NOW(),
    Updated_By = 'SYSTEM';

-- Future Value Date Limit (default: 30 days)
INSERT INTO Parameter_Table (Parameter_Name, Parameter_Value, Parameter_Description, Last_Updated, Updated_By)
VALUES ('Future_Value_Date_Limit_Days', '30', 'Maximum days in future for value dating', NOW(), 'SYSTEM')
ON DUPLICATE KEY UPDATE
    Parameter_Description = 'Maximum days in future for value dating',
    Last_Updated = NOW(),
    Updated_By = 'SYSTEM';

-- Interest Default Divisor (default: 36500)
-- Formula: 365 days × 100 (for percentage-based interest calculation)
INSERT INTO Parameter_Table (Parameter_Name, Parameter_Value, Parameter_Description, Last_Updated, Updated_By)
VALUES ('Interest_Default_Divisor', '36500', 'Divisor for interest calculation (365 * 100)', NOW(), 'SYSTEM')
ON DUPLICATE KEY UPDATE
    Parameter_Description = 'Divisor for interest calculation (365 * 100)',
    Last_Updated = NOW(),
    Updated_By = 'SYSTEM';

-- Last End of Month Date
-- This should be updated by the system after each month-end closing
-- Default set to a reasonable past date
INSERT INTO Parameter_Table (Parameter_Name, Parameter_Value, Parameter_Description, Last_Updated, Updated_By)
VALUES ('Last_EOM_Date', '2024-12-31', 'Last End of Month date (updated after month-end closing)', NOW(), 'SYSTEM')
ON DUPLICATE KEY UPDATE
    Parameter_Description = 'Last End of Month date (updated after month-end closing)',
    Last_Updated = NOW(),
    Updated_By = 'SYSTEM';
