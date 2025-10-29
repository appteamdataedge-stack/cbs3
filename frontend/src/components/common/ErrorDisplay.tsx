import { Alert, AlertTitle, Box, Button, Collapse, Typography } from '@mui/material';
import { useState } from 'react';
import { ErrorOutline, Refresh } from '@mui/icons-material';

interface ErrorDisplayProps {
  error: Error | unknown;
  title?: string;
  onRetry?: () => void;
  showDetails?: boolean;
}

/**
 * A component to display errors in a consistent way across the application
 */
const ErrorDisplay = ({ 
  error, 
  title = 'Error', 
  onRetry,
  showDetails = false
}: ErrorDisplayProps) => {
  const [expanded, setExpanded] = useState(false);
  
  // Extract error message
  const errorMessage = error instanceof Error 
    ? error.message 
    : 'An unknown error occurred';
  
  // Check if it's a network error
  const isNetworkError = errorMessage.includes('Network Error') || 
    errorMessage.includes('connect') || 
    errorMessage.includes('ECONNREFUSED');

  return (
    <Alert 
      severity="error"
      icon={<ErrorOutline />}
      action={
        onRetry && (
          <Button 
            color="inherit" 
            size="small" 
            onClick={onRetry}
            startIcon={<Refresh />}
          >
            Retry
          </Button>
        )
      }
      sx={{ mb: 2 }}
    >
      <AlertTitle>{title}</AlertTitle>
      
      {isNetworkError ? (
        <Typography variant="body2">
          Cannot connect to the server. Please check if the backend is running.
        </Typography>
      ) : (
        <Typography variant="body2">
          {errorMessage}
        </Typography>
      )}

      {showDetails && error instanceof Error && error.stack && (
        <Box mt={1}>
          <Button 
            size="small" 
            color="inherit" 
            onClick={() => setExpanded(!expanded)}
          >
            {expanded ? 'Hide Details' : 'Show Details'}
          </Button>
          <Collapse in={expanded}>
            <Box 
              mt={1} 
              p={1} 
              bgcolor="rgba(0,0,0,0.05)" 
              borderRadius={1}
              sx={{ overflowX: 'auto' }}
            >
              <Typography variant="caption" component="pre" style={{ whiteSpace: 'pre-wrap' }}>
                {error.stack}
              </Typography>
            </Box>
          </Collapse>
        </Box>
      )}
    </Alert>
  );
};

export default ErrorDisplay;
