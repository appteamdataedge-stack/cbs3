import {
  Box,
  Card,
  CardContent,
  TextField,
  Button,
  Typography,
  Alert,
  Grid,
  Paper
} from '@mui/material';
import { useState, useEffect } from 'react';
import { toast } from 'react-toastify';
import { PageHeader } from '../../components/common';
import { apiRequest } from '../../api/apiClient';

const SystemDate = () => {
  // Get current date as default
  const today = new Date().toISOString().split('T')[0];
  const [systemDate, setSystemDate] = useState<string>(today);
  const [currentSystemDate, setCurrentSystemDate] = useState<string>(today);
  const [isUpdating, setIsUpdating] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

  // Fetch current system date from backend
  const fetchCurrentSystemDate = async () => {
    try {
      const response = await apiRequest<{systemDate: string}>({
        method: 'GET',
        url: '/admin/eod/status'
      });
      setCurrentSystemDate(response.systemDate);
      setSystemDate(response.systemDate);
    } catch (error) {
      console.error('Failed to fetch current system date:', error);
      toast.error('Failed to fetch current system date');
    } finally {
      setIsLoading(false);
    }
  };

  // Load current system date on component mount
  useEffect(() => {
    fetchCurrentSystemDate();
  }, []);

  // Handle date change
  const handleDateChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setSystemDate(event.target.value);
  };

  // Update system date
  const handleUpdateSystemDate = async () => {
    if (!systemDate) {
      toast.error('Please select a valid date');
      return;
    }

    setIsUpdating(true);
    
    try {
      // Call the backend API to update system date
      const response = await apiRequest<{success: boolean, message: string, systemDate: string}>({
        method: 'POST',
        url: `/admin/set-system-date?systemDateStr=${systemDate}`
      });
      
      if (response.success) {
        // Update the current system date display
        setCurrentSystemDate(response.systemDate);
        toast.success(`System date updated to ${response.systemDate}`);
      } else {
        toast.error('Failed to update system date');
      }
    } catch (error: any) {
      console.error('Failed to update system date:', error);
      toast.error(`Failed to update system date: ${error.message || 'Unknown error'}`);
    } finally {
      setIsUpdating(false);
    }
  };

  return (
    <Box>
      <PageHeader title="System Date Management" />

      <Alert severity="info" sx={{ mb: 3 }}>
        This module allows you to set and manage the system date for testing purposes. 
        The system date is stored in the Parameter_Table and used by all batch jobs and transactions.
        Changes will be immediately reflected across the entire system.
      </Alert>

      <Grid container spacing={3}>
        {/* Current System Date Display */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Current System Date
              </Typography>
              <Paper 
                variant="outlined" 
                sx={{ 
                  p: 2, 
                  backgroundColor: 'primary.50',
                  border: '2px solid',
                  borderColor: 'primary.main'
                }}
              >
                <Typography variant="h4" color="primary.main" align="center">
                  {isLoading ? 'Loading...' : currentSystemDate}
                </Typography>
                <Typography variant="body2" color="text.secondary" align="center" sx={{ mt: 1 }}>
                  This is the date used by all batch jobs and transactions
                </Typography>
              </Paper>
            </CardContent>
          </Card>
        </Grid>

        {/* System Date Update */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Update System Date
              </Typography>
              <Typography variant="body2" color="text.secondary" paragraph>
                Set a new system date for testing purposes. This will affect all batch jobs and transactions.
              </Typography>
              
              <TextField
                label="System Date"
                type="date"
                value={systemDate}
                onChange={handleDateChange}
                fullWidth
                margin="normal"
                InputLabelProps={{ shrink: true }}
                helperText="Select the date to be used as system date"
              />

              <Box sx={{ mt: 2, display: 'flex', justifyContent: 'flex-end' }}>
                <Button
                  variant="contained"
                  color="primary"
                  onClick={handleUpdateSystemDate}
                  disabled={isUpdating}
                >
                  {isUpdating ? 'Updating...' : 'Update System Date'}
                </Button>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Information Card */}
        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                About System Date
              </Typography>
              <Typography variant="body2" color="text.secondary" paragraph>
                The system date is a critical component in the Money Market system:
              </Typography>
              <Box component="ul" sx={{ pl: 2 }}>
                <Typography component="li" variant="body2" paragraph>
                  <strong>Batch Jobs:</strong> All EOD batch jobs use this date to process transactions and update balances
                </Typography>
                <Typography component="li" variant="body2" paragraph>
                  <strong>Transaction Processing:</strong> New transactions are dated with the system date
                </Typography>
                <Typography component="li" variant="body2" paragraph>
                  <strong>Interest Calculations:</strong> Interest accruals are calculated based on this date
                </Typography>
                <Typography component="li" variant="body2" paragraph>
                  <strong>Reporting:</strong> All reports and statements use this date as the reference point
                </Typography>
              </Box>
              <Alert severity="warning" sx={{ mt: 2 }}>
                <strong>Important:</strong> Changing the system date should only be done during testing or maintenance windows. 
                In production, the system date should automatically advance daily.
              </Alert>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
};

export default SystemDate;
