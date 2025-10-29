import { useState } from 'react';
import { Box, Button, Typography, Paper, CircularProgress, Alert } from '@mui/material';
import { getAllSubProducts } from '../api/subProductService';
import { getAllCustomerAccounts } from '../api/customerAccountService';

const ApiDebug = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [responseData, setResponseData] = useState<any>(null);

  const testSubProducts = async () => {
    setLoading(true);
    setError(null);
    setSuccess(null);
    setResponseData(null);

    try {
      console.log('Testing SubProducts API...');
      const response = await getAllSubProducts(0, 10);
      console.log('SubProducts API Response:', response);
      setSuccess('Successfully fetched SubProducts data');
      setResponseData(response);
    } catch (err: any) {
      console.error('SubProducts API Error:', err);
      setError(`Error fetching SubProducts: ${err.message || 'Unknown error'}`);
    } finally {
      setLoading(false);
    }
  };

  const testAccounts = async () => {
    setLoading(true);
    setError(null);
    setSuccess(null);
    setResponseData(null);

    try {
      console.log('Testing Accounts API...');
      const response = await getAllCustomerAccounts(0, 10);
      console.log('Accounts API Response:', response);
      setSuccess('Successfully fetched Accounts data');
      setResponseData(response);
    } catch (err: any) {
      console.error('Accounts API Error:', err);
      setError(`Error fetching Accounts: ${err.message || 'Unknown error'}`);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box sx={{ p: 4 }}>
      <Typography variant="h4" gutterBottom>
        API Debug Page
      </Typography>

      <Typography variant="body1" paragraph>
        Use this page to test API connections for SubProducts and Accounts.
      </Typography>

      <Box sx={{ display: 'flex', gap: 2, mb: 3 }}>
        <Button 
          variant="contained" 
          onClick={testSubProducts}
          disabled={loading}
        >
          Test SubProducts API
        </Button>
        
        <Button 
          variant="contained" 
          onClick={testAccounts}
          disabled={loading}
        >
          Test Accounts API
        </Button>
      </Box>

      {loading && (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <CircularProgress size={20} />
          <Typography>Testing API connection...</Typography>
        </Box>
      )}

      {error && (
        <Alert severity="error" sx={{ mt: 2 }}>
          {error}
        </Alert>
      )}

      {success && (
        <Alert severity="success" sx={{ mt: 2 }}>
          {success}
        </Alert>
      )}

      {responseData && (
        <Paper sx={{ mt: 3, p: 2, maxHeight: '400px', overflow: 'auto', bgcolor: '#f5f5f5' }}>
          <Typography variant="subtitle1" gutterBottom>Response Data:</Typography>
          <pre style={{ whiteSpace: 'pre-wrap' }}>
            {JSON.stringify(responseData, null, 2)}
          </pre>
        </Paper>
      )}
    </Box>
  );
};

export default ApiDebug;
