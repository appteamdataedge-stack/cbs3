import { Outlet } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { Box, Typography, Button } from '@mui/material';
import LockIcon from '@mui/icons-material/Lock';

interface ProtectedRouteProps {
  requiredPermission?: string;
  requiredRole?: string;
}

/**
 * Protected route component that checks authentication and authorization
 */
const ProtectedRoute = ({ requiredPermission, requiredRole }: ProtectedRouteProps) => {
  const { hasPermission, hasRole, logout } = useAuth();

  // Check if user is logged in
  // Temporarily bypass authentication check for development
  // if (!isLoggedIn) {
  //   return <Navigate to="/login" replace />;
  // }

  // Check permissions if required
  // Temporarily bypass permission check for development
  if (requiredPermission && !hasPermission(requiredPermission || '')) {
    return (
      <Box
        sx={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          minHeight: '50vh',
          textAlign: 'center',
          p: 3,
        }}
      >
        <LockIcon sx={{ fontSize: 60, color: 'error.main', mb: 2 }} />
        <Typography variant="h4" component="h1" gutterBottom>
          Access Denied
        </Typography>
        <Typography variant="body1" color="text.secondary" paragraph>
          You don't have permission to access this page.
        </Typography>
        <Box sx={{ mt: 2 }}>
          <Button variant="contained" color="primary" onClick={() => window.history.back()}>
            Go Back
          </Button>
          <Button variant="outlined" color="secondary" onClick={logout} sx={{ ml: 2 }}>
            Logout
          </Button>
        </Box>
      </Box>
    );
  }

  // Check role if required
  // Temporarily bypass role check for development
  if (requiredRole && !hasRole(requiredRole || '')) {
    return (
      <Box
        sx={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          minHeight: '50vh',
          textAlign: 'center',
          p: 3,
        }}
      >
        <LockIcon sx={{ fontSize: 60, color: 'error.main', mb: 2 }} />
        <Typography variant="h4" component="h1" gutterBottom>
          Access Denied
        </Typography>
        <Typography variant="body1" color="text.secondary" paragraph>
          Your role doesn't have access to this page.
        </Typography>
        <Box sx={{ mt: 2 }}>
          <Button variant="contained" color="primary" onClick={() => window.history.back()}>
            Go Back
          </Button>
          <Button variant="outlined" color="secondary" onClick={logout} sx={{ ml: 2 }}>
            Logout
          </Button>
        </Box>
      </Box>
    );
  }

  // User is authenticated and authorized
  return <Outlet />;
};

export default ProtectedRoute;
