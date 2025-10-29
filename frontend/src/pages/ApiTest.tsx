import { useState } from 'react';
import { Box, Button, Typography, Paper, CircularProgress, Alert, TextField } from '@mui/material';
import axios from 'axios';

// Default API URL - can be changed by the user
const DEFAULT_API_URL = `${import.meta.env.VITE_API_URL || 'http://localhost:8082'}/api`;

const ApiTest = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [apiUrl, setApiUrl] = useState(DEFAULT_API_URL);
  const [responseData, setResponseData] = useState<any>(null);

  const testApi = async (endpoint: string) => {
    setLoading(true);
    setError(null);
    setSuccess(null);
    setResponseData(null);

    try {
      const fullUrl = `${apiUrl}${endpoint}`;
      console.log('Testing API URL:', fullUrl);
      
      const response = await axios.get(fullUrl);
      console.log('API Response:', response.data);
      setSuccess(`Successfully connected to ${endpoint}`);
      setResponseData(response.data);
    } catch (err: any) {
      console.error('API Error:', err);
      
      // Detailed error information
      let errorMessage = 'Unknown error occurred';
      
      if (err.message) {
        errorMessage = err.message;
      }
      
      if (err.response) {
        // The request was made and the server responded with a status code
        errorMessage = `Server Error: ${err.response.status} - ${err.response.statusText}`;
        setResponseData(err.response.data);
      } else if (err.request) {
        // The request was made but no response was received
        errorMessage = 'Network Error: No response received from server. Check if the server is running and accessible.';
      }
      
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box sx={{ p: 4 }}>
      <Typography variant="h4" gutterBottom>
        API Connectivity Test
      </Typography>

      <Paper sx={{ p: 3, mb: 3 }}>
        <Typography variant="h6" gutterBottom>
          API Configuration
        </Typography>
        
        <TextField
          fullWidth
          label="API Base URL"
          value={apiUrl}
          onChange={(e) => setApiUrl(e.target.value)}
          margin="normal"
          variant="outlined"
          helperText="Change this if your backend is running on a different port"
        />

        <Typography variant="h6" sx={{ mt: 3 }} gutterBottom>
          Test API Endpoints
        </Typography>

        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2, mb: 3 }}>
          <Button 
            variant="contained" 
            onClick={() => testApi('/customers')}
            disabled={loading}
          >
            Test Customers API
          </Button>
          
          <Button 
            variant="contained" 
            onClick={() => testApi('/products')}
            disabled={loading}
          >
            Test Products API
          </Button>
          
          <Button 
            variant="contained" 
            onClick={() => testApi('/subproducts')}
            disabled={loading}
          >
            Test SubProducts API
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
          <Paper sx={{ p: 2, mt: 2, maxHeight: '300px', overflow: 'auto', bgcolor: '#f5f5f5' }}>
            <Typography variant="subtitle2" gutterBottom>Response Data:</Typography>
            <pre style={{ whiteSpace: 'pre-wrap', fontSize: '0.85rem' }}>
              {JSON.stringify(responseData, null, 2)}
            </pre>
          </Paper>
        )}
      </Paper>

      <Typography variant="body2" color="text.secondary">
        Check the browser console for additional API response details.
      </Typography>
    </Box>
  );
};

export default ApiTest;
