import { useState } from 'react';
import { Box, Button, Typography, Paper, CircularProgress, Alert } from '@mui/material';
import axios from 'axios';

const CorsTest = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [response, setResponse] = useState<any>(null);

  const testEndpoint = async (endpoint: string) => {
    setLoading(true);
    setError(null);
    setSuccess(null);
    setResponse(null);

    try {
      // Use direct axios call to test CORS without any interceptors
      const result = await axios.get(`${import.meta.env.VITE_API_URL || 'http://localhost:8082'}${endpoint}`, {
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json'
        },
        withCredentials: true
      });
      
      setSuccess(`Successfully connected to ${endpoint}`);
      setResponse(result.data);
      console.log('Response:', result);
    } catch (err: any) {
      console.error('Error details:', err);
      
      let errorMessage = 'Unknown error';
      if (err.message) {
        errorMessage = err.message;
      }
      
      if (err.response) {
        errorMessage = `Server responded with status ${err.response.status}: ${err.response.statusText}`;
        setResponse(err.response.data);
      } else if (err.request) {
        errorMessage = 'No response received from server';
      }
      
      setError(`CORS Test Error: ${errorMessage}`);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box sx={{ p: 4 }}>
      <Typography variant="h4" gutterBottom>
        CORS Test Page
      </Typography>
      
      <Paper sx={{ p: 3, mb: 3 }}>
        <Typography variant="body1" paragraph>
          This page tests if CORS is properly configured between frontend and backend.
        </Typography>
        
        <Box sx={{ display: 'flex', gap: 2, mb: 3 }}>
          <Button 
            variant="contained" 
            onClick={() => testEndpoint('/api/customers')}
            disabled={loading}
          >
            Test Customers API
          </Button>
          
          <Button 
            variant="contained" 
            onClick={() => testEndpoint('/api/products')}
            disabled={loading}
          >
            Test Products API
          </Button>
          
          <Button 
            variant="contained" 
            onClick={() => testEndpoint('/api')}
            disabled={loading}
          >
            Test API Root
          </Button>
        </Box>
        
        {loading && (
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <CircularProgress size={20} />
            <Typography>Testing CORS configuration...</Typography>
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
        
        {response && (
          <Box sx={{ mt: 2, p: 2, bgcolor: '#f5f5f5', borderRadius: 1, overflow: 'auto', maxHeight: 300 }}>
            <Typography variant="subtitle2" gutterBottom>Response:</Typography>
            <pre>{JSON.stringify(response, null, 2)}</pre>
          </Box>
        )}
      </Paper>
      
      <Typography variant="body2" color="text.secondary">
        If you see CORS errors in the console, the backend CORS configuration needs to be fixed.
      </Typography>
    </Box>
  );
};

export default CorsTest;
