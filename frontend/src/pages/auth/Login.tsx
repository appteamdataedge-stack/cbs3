import { useState } from 'react';
import { useNavigate, Navigate } from 'react-router-dom';
import {
  Box,
  Button,
  TextField,
  Typography,
  Paper,
  Container,
  InputAdornment,
  IconButton,
  Alert,
  Link,
  Divider,
  CircularProgress
} from '@mui/material';
import { Visibility, VisibilityOff, LockOutlined, AccountCircle } from '@mui/icons-material';
import { useAuth } from '../../context/AuthContext';
import { toast } from 'react-toastify';

// Mock login function - replace with actual API call
const mockLogin = async (username: string, password: string) => {
  // Simulate API call
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      // For demo purposes only - in production, this would be an actual API call
      if (username === 'admin' && password === 'password') {
        resolve({
          token: 'mock-jwt-token',
          user: {
            id: 1,
            username: 'admin',
            email: 'admin@example.com',
            role: 'ADMIN',
            permissions: ['READ', 'WRITE', 'DELETE', 'APPROVE'],
            fullName: 'Administrator'
          },
          expiresIn: 3600
        });
      } else {
        reject(new Error('Invalid username or password'));
      }
    }, 1000);
  });
};

const Login = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  
  const navigate = useNavigate();
  const { login, isLoggedIn } = useAuth();
  
  // If already logged in, redirect to dashboard
  if (isLoggedIn) {
    return <Navigate to="/" replace />;
  }
  
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    
    if (!username || !password) {
      setError('Please enter both username and password');
      return;
    }
    
    try {
      setLoading(true);
      const response: any = await mockLogin(username, password);
      
      // Login successful
      login(response.token, response.user, response.expiresIn);
      toast.success('Login successful!');
      navigate('/');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Login failed');
      setLoading(false);
    }
  };
  
  return (
    <Container component="main" maxWidth="sm" sx={{ mt: 8 }}>
      <Paper 
        elevation={3} 
        sx={{ 
          p: 4, 
          display: 'flex', 
          flexDirection: 'column', 
          alignItems: 'center',
          borderRadius: 2
        }}
      >
        <Box 
          sx={{ 
            bgcolor: 'primary.main', 
            color: 'white', 
            p: 2, 
            borderRadius: '50%',
            mb: 2
          }}
        >
          <LockOutlined fontSize="large" />
        </Box>
        
        <Typography component="h1" variant="h5" fontWeight="bold">
          Money Market System
        </Typography>
        
        <Typography variant="subtitle1" color="text.secondary" sx={{ mb: 3 }}>
          Sign in to your account
        </Typography>
        
        {error && (
          <Alert severity="error" sx={{ width: '100%', mb: 3 }}>
            {error}
          </Alert>
        )}
        
        <Box component="form" onSubmit={handleSubmit} sx={{ width: '100%' }}>
          <TextField
            margin="normal"
            required
            fullWidth
            id="username"
            label="Username"
            name="username"
            autoComplete="username"
            autoFocus
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <AccountCircle color="primary" />
                </InputAdornment>
              ),
            }}
          />
          
          <TextField
            margin="normal"
            required
            fullWidth
            name="password"
            label="Password"
            type={showPassword ? 'text' : 'password'}
            id="password"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <LockOutlined color="primary" />
                </InputAdornment>
              ),
              endAdornment: (
                <InputAdornment position="end">
                  <IconButton
                    aria-label="toggle password visibility"
                    onClick={() => setShowPassword(!showPassword)}
                    edge="end"
                  >
                    {showPassword ? <VisibilityOff /> : <Visibility />}
                  </IconButton>
                </InputAdornment>
              ),
            }}
          />
          
          <Box sx={{ display: 'flex', justifyContent: 'flex-end', mt: 1 }}>
            <Link href="#" variant="body2">
              Forgot password?
            </Link>
          </Box>
          
          <Button
            type="submit"
            fullWidth
            variant="contained"
            sx={{ mt: 3, mb: 2, py: 1.5 }}
            disabled={loading}
          >
            {loading ? <CircularProgress size={24} /> : 'Sign In'}
          </Button>
          
          <Divider sx={{ my: 2 }}>
            <Typography variant="body2" color="text.secondary">
              Demo Credentials
            </Typography>
          </Divider>
          
          <Box sx={{ textAlign: 'center', mb: 2 }}>
            <Typography variant="body2" color="text.secondary">
              Username: <strong>admin</strong>, Password: <strong>password</strong>
            </Typography>
          </Box>
        </Box>
      </Paper>
      
      <Box sx={{ mt: 2, textAlign: 'center' }}>
        <Typography variant="body2" color="text.secondary">
          &copy; {new Date().getFullYear()} Money Market System. All rights reserved.
        </Typography>
      </Box>
    </Container>
  );
};

export default Login;
