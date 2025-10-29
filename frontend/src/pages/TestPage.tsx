import { Box, Typography, Button } from '@mui/material';

const TestPage = () => {
  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: '60vh',
        textAlign: 'center',
        p: 3,
      }}
    >
      <Typography variant="h2" component="h1" gutterBottom>
        Test Page
      </Typography>
      <Typography variant="body1" paragraph>
        If you can see this, the frontend is working correctly.
      </Typography>
      <Button variant="contained" color="primary" onClick={() => alert('Button clicked!')}>
        Click Me
      </Button>
    </Box>
  );
};

export default TestPage;
