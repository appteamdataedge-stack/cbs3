import { createContext, useContext, useState, useEffect } from 'react';
import type { ReactNode } from 'react';
import { getUserData, isAuthenticated, clearAuthToken, setAuthData } from '../utils/authUtils';
import type { UserData } from '../utils/authUtils';

// Default admin user for development
const getDefaultAdminUser = (): UserData => ({
  id: 'admin',
  username: 'admin',
  email: 'admin@moneymarket.com',
  role: 'ADMIN',
  permissions: ['*'], // All permissions
  fullName: 'Admin User'
});

// Define context types
interface AuthContextType {
  isLoggedIn: boolean;
  user: UserData | null;
  login: (token: string, userData: UserData, expiresIn?: number) => void;
  logout: () => void;
  hasPermission: (permission: string) => boolean;
  hasRole: (role: string) => boolean;
  setDefaultAdmin: () => void;
}

// Create context with default values
const AuthContext = createContext<AuthContextType>({
  isLoggedIn: false,
  user: null,
  login: () => {},
  logout: () => {},
  hasPermission: () => false,
  hasRole: () => false,
  setDefaultAdmin: () => {},
});

// Provider component
interface AuthProviderProps {
  children: ReactNode;
}

export const AuthProvider = ({ children }: AuthProviderProps) => {
  const [isLoggedIn, setIsLoggedIn] = useState<boolean>(isAuthenticated());
  const [user, setUser] = useState<UserData | null>(getUserData() || getDefaultAdminUser());

  // Check authentication status on mount
  useEffect(() => {
    const checkAuth = () => {
      setIsLoggedIn(isAuthenticated());
      setUser(getUserData() || getDefaultAdminUser());
    };

    // Check auth initially
    checkAuth();

    // Set up interval to periodically check authentication
    const interval = setInterval(checkAuth, 60000); // Check every minute

    // Event listener for storage changes (for multi-tab support)
    const handleStorageChange = () => {
      checkAuth();
    };

    window.addEventListener('storage', handleStorageChange);

    return () => {
      clearInterval(interval);
      window.removeEventListener('storage', handleStorageChange);
    };
  }, []);

  // Login function
  const login = (token: string, userData: UserData, expiresIn?: number) => {
    setAuthData(token, userData, expiresIn);
    setIsLoggedIn(true);
    setUser(userData);
  };

  // Set default admin user for development
  const setDefaultAdmin = () => {
    const adminUser = getDefaultAdminUser();
    setUser(adminUser);
    setIsLoggedIn(true);
  };

  // Logout function
  const logout = () => {
    clearAuthToken();
    setIsLoggedIn(false);
    setUser(null);
    window.location.href = '/login';
  };

  // Check if user has specific permission
  const hasPermission = (permission: string): boolean => {
    if (!user || !user.permissions) {
      return false;
    }
    return user.permissions.includes(permission);
  };

  // Check if user has specific role
  const hasRole = (role: string): boolean => {
    if (!user) {
      return false;
    }
    return user.role === role;
  };

  const value = {
    isLoggedIn,
    user,
    login,
    logout,
    hasPermission,
    hasRole,
    setDefaultAdmin,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

// Custom hook for using auth context
export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export default AuthContext;
